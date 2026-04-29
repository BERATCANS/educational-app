# Migration Deploy-Readiness Checklist (V8 → V17)

> Tarih: 2026-04-27 · Hedef: prod V7'den V17'ye yükseltme
> Bu doküman: V8'den V17'ye kadar olan Flyway migration zincirinin prod'a uygulanmadan önce doğrulanması gereken noktaları içerir.

## TL;DR

V8–V17 zinciri prod'a uygulanabilir, ama:
1. **DB backup şart** (V9, V14 DROP COLUMN içerir — geri alınamaz).
2. Aşağıdaki **4 sayım sorgusu** deploy öncesi 0 dönmeli (V14 ve V17 NOT NULL/DELETE adımları için).
3. **Maintenance window** içinde, app durdurulmuş halde Flyway koştur.
4. Staging/replica'da provası yapılmadan prod'a basma.

---

## Migration başına risk haritası

| Migration | Risk | Özet |
|---|---|---|
| V8 | ✅ Yok | `purchases.transaction_id` UNIQUE constraint düşürülüyor — geri alınabilir. |
| V9 | ⚠️ Latent (bu deploy'da tetiklenmez) | Multi-row purchase deduplication mantığı kusurlu (CASCADE ile child satırlar silinebilir). Prod V7'de aynı `transaction_id`'li çoklu satır olmadığı için DELETE 0 satır siler — tehlike pasif. |
| V10 | ✅ Yok | Yeni `discount_code_users` tablosu. |
| V11 | ✅ Yok | `discount_codes.publisher_id` → join tabloya taşınır. PG `DROP COLUMN`'da FK otomatik düşer. |
| V12 | ✅ Yok | `is_deleted BOOLEAN DEFAULT FALSE NOT NULL` 8 tabloya eklenir. |
| V13 | ✅ Yok | 4 yeni nullable password-reset kolonu. |
| V14 | 🟡 Edge case | User columns drop edilir; backfill çoğu hesabı kapsar ama "non-STUDENT user'da student verisi" senaryosunda kayıp olabilir. NOT NULL adımları backfill başarılıysa geçer. |
| V15 | 🟡 Scale | `parent_invite_code` MD5 ilk 6 char (16M kombinasyon). Çok öğrenci varsa UNIQUE çakışma olasılığı yükselir. |
| V16 | ✅ Yok | `classrooms.creator_teacher_profile_id` NOT NULL → nullable, FK SET NULL. |
| V17 | 🟡 Şartlı | Yetim cart'ları (TEACHER/PARENT/ADMIN için yanlışlıkla oluşmuşsa) siler. Sayım 0 ise tamamen güvenli. |

---

## Deploy öncesi sayım sorguları

Hepsi **0** dönmelidir.

### 1) V14 backfill bütünlüğü

```sql
-- test_results / book_reviews / daily_question_answers'da
-- user_id'si bir user satırına denk gelmeyen orphan satır var mı?
SELECT COUNT(*) FROM test_results tr
  LEFT JOIN users u ON u.id = tr.user_id
WHERE u.id IS NULL;

SELECT COUNT(*) FROM book_reviews br
  LEFT JOIN users u ON u.id = br.user_id
WHERE u.id IS NULL;

SELECT COUNT(*) FROM daily_question_answers dqa
  LEFT JOIN users u ON u.id = dqa.user_id
WHERE u.id IS NULL;
```
> 0 değilse → o orphan satırları sil veya FK'yi düzelt; aksi halde V14'te `SET NOT NULL` patlar.

### 2) V14 user-column data loss kontrolü

```sql
-- STUDENT olmayıp da öğrenci alanları doldurulmuş user var mı?
-- (Bu user'lar V14 backfill kapsamı dışındaysa veri kaybeder.)
SELECT COUNT(*) FROM users
WHERE role <> 'STUDENT'
  AND (current_streak > 0
       OR last_activity_date IS NOT NULL
       OR daily_solved_questions > 0
       OR grade_level IS NOT NULL
       OR exam_type IS NOT NULL
       OR field_type IS NOT NULL
       OR school_type IS NOT NULL);
```
> 0 değilse → V14 section 2.1 bu kullanıcıları kapsayıp kapsamadığını kontrol et (test_results/book_reviews/daily_question_answers/user_flagged_questions'da satırı varsa kapsanır). Yoksa veri kaybı kabul edilebilir mi karar ver.

### 3) V17 yetim cart kontrolü

```sql
-- Sepet sahibi user'ı bir student_profile'a denk gelmiyorsa
-- bu cart V17 ile silinir (cart_items CASCADE ile birlikte).
SELECT c.id AS cart_id, c.user_id, u.role
FROM carts c
LEFT JOIN users u ON u.id = c.user_id
LEFT JOIN student_profiles sp ON sp.user_id = c.user_id
WHERE sp.id IS NULL;
```
> 0 satır olmalı. Değilse:
> - Eğer user gerçekten STUDENT ama profili yoksa: önce o user için `student_profiles` satırı oluştur.
> - Eğer user TEACHER/PARENT/ADMIN ise: cart'ın bilinçli olup olmadığını araştır; muhtemelen yanlışlıkla oluşmuş, silinmesi doğru.

### 4) V15 collision riski (öğrenci sayısı)

```sql
SELECT COUNT(*) FROM users WHERE role = 'STUDENT';
```
> ≤ 500: güvenli (~%0.7 collision).
> 500–2000: %3–%12 — UNIQUE çakışma migration'ı patlatabilir, retry önerilir.
> > 2000: V15'i prod'a uygulamadan önce kod güncelle: 6-char yerine 8-char veya UUID-based üretim.

### 5) Ek kontrol — V9 idempotency

```sql
-- Aynı transaction_id'li çoklu purchase satırı var mı?
-- Prod V7'de UNIQUE constraint var, 0 dönmeli.
SELECT transaction_id, COUNT(*) FROM purchases
WHERE transaction_id IS NOT NULL
GROUP BY transaction_id
HAVING COUNT(*) > 1;
```
> 0 dönmezse V9 step 7 cascade ile child satırlarını siler — **veri kaybı**. Bu durumda V9'u manuel patch'le (önce purchase_items.purchase_id'yi MIN(id)'ye repoint et).

---

## Deploy adımları

```bash
# 1. Backup
pg_dump -h <prod-host> -U <user> -d <db> > backup_$(date +%F_%H%M).sql

# 2. Yukarıdaki 5 sayım sorgusunu koştur, hepsi beklenen değerde mi?
#    Beklenmedik sonuç → STOP, müdahale gerek.

# 3. Staging/replica'ya migration'ı önce uygula:
SPRING_PROFILES_ACTIVE=staging ./mvnw flyway:migrate
SPRING_PROFILES_ACTIVE=staging ./mvnw spring-boot:run
# → Schema validate başarılı mı? Smoke test (login, sepete ekle, checkout)?

# 4. Maintenance window içinde prod:
#    a) App'i durdur (mevcut request'lerin drain olmasını bekle)
#    b) Backup henüz alındı mı doğrula
#    c) Migration'ı koştur:
SPRING_PROFILES_ACTIVE=prod ./mvnw flyway:migrate
#    d) Yeni app'i başlat (ddl-auto=validate ile şema kontrolü zaten ilk açılışta yapılır)
SPRING_PROFILES_ACTIVE=prod ./mvnw spring-boot:run

# 5. Smoke test:
#    - /api/users/login
#    - /api/cart (boş döner)
#    - /api/cart/add (öğrenci hesabı)
#    - /api/cart/checkout (mock payment ile)
#    - /api/users/use-code (yeni pessimistic lock'lu kod akışı)
```

---

## Rollback stratejisi

Migration zinciri yarıda patlarsa:

1. **V8 / V10 / V11 / V12 / V13 / V15 / V16 başarılı, V14 fail**:
   - V14 PG transaction'ında olduğu için kendi rollback eder. Yapılan kısmi değişiklik yok.
   - Sayım sorgularıyla kök sebebi bul, düzelt, yeniden çalıştır.

2. **V14 başarılı, V17 fail**:
   - V14 commit'lenmiş; user-spesifik student kolonları kalıcı olarak gitmiş. **Geri alma için backup'tan restore gerekir.**

3. **Tamamı başarılı ama app start fail**:
   - Hibernate `validate` hatası → bir entity ile DB arasında uyuşmazlık var demektir.
   - Backup'tan restore + Java tarafında düzeltme + tekrar deploy.

**Kural**: V14 başladıktan sonra rollback'in tek güvenilir yolu `pg_restore`'dur. O yüzden adım 1 (backup) atlanmamalı.

---

## Bilinen latent issue (gelecek için)

**V9'un içindeki cascade-delete bug'ı** (multi-row state'te purchase_items kaybı): bu deploy'u tehdit etmiyor (V7-prod'da multi-row state yok), ama V9 dosyasının başına bir uyarı yorumu eklenmesi önerilir:

```sql
-- WARNING: Bu migration sadece V8 öncesi prod state'inde güvenlidir
-- (transaction_id UNIQUE => transaction başına 1 satır). Multi-row state'te
-- step 7 DELETE, ON DELETE CASCADE üzerinden purchase_items satırlarını
-- da siler. Multi-row state olursa migration'ı manuel patch'le:
-- önce UPDATE purchase_items SET purchase_id = MIN(...) BY transaction_id.
```

Mevcut V9 dosyasına yorum eklemek Flyway checksum'ını değiştirir (zaten dev'de uygulanmış migration'lar için checksum hatası verir). O yüzden sadece bu doküman içinde tutuldu.
