# Test: Soru Yükleme ve AI Analiz

Bu doküman, yeni soru yükleme sisteminin nasıl test edileceğini açıklar.

## Ön Gereksinimler

1. **JWT Token**: Admin kullanıcısı için geçerli bir JWT token
2. **Test ID**: Mevcut bir test ID'si
3. **Görsel Dosyası**: Test edilecek soru görseli (PNG, JPG, vb.)

## Test Senaryosu

### Senaryo: Matematik Sorusu Yükleme

**Soru**: İşlem tablosu problemi (a, b, c tamsayıları ile işlem tablosu)

**Beklenen Sonuç**:

- Soru başarıyla oluşturulur
- AI analiz otomatik yapılır
- 3 hint otomatik eklenir
- Explanation otomatik eklenir
- Admin hints ve explanation'ı düzenleyebilir

## Test Yöntemleri

### Yöntem 1: Shell Script (Önerilen)

```bash
# 1. JWT token al (login yaparak)
curl -X POST http://localhost:8080/api/users/login \
  -H "Content-Type: application/json" \
  -d '{
    "identifier": "admin",
    "password": "admin123"
  }'

# 2. Token'ı kopyala ve script'i çalıştır
./test-question-upload.sh \
  "YOUR_JWT_TOKEN_HERE" \
  1 \
  "/path/to/question-image.png"
```

### Yöntem 2: cURL Komutları (Manuel)

#### Adım 1: Soru Yükle (Otomatik AI Analiz)

```bash
curl -X POST http://localhost:8080/api/questions/add \
  -H "Authorization: Bearer YOUR_JWT_TOKEN" \
  -F "file=@/path/to/question-image.png" \
  -F "testId=1" \
  -F "correctAnswer=A" \
  -F "answerNumber=5" \
  -F "sequenceNumber=1"
```

**Beklenen Response:**

```json
{
  "id": 123,
  "imageUrl": "1234567890_question-image.png",
  "correctAnswer": "A",
  "answerNumber": 5,
  "sequenceNumber": 1,
  "hints": [
    "AI tarafından üretilen ipucu 1",
    "AI tarafından üretilen ipucu 2",
    "AI tarafından üretilen ipucu 3"
  ],
  "explanation": "AI tarafından üretilen çözüm açıklaması",
  "test": {
    "id": 1,
    "title": "Test Başlığı"
  }
}
```

#### Adım 2: Soru Detaylarını Getir

```bash
curl -X GET http://localhost:8080/api/questions/123 \
  -H "Authorization: Bearer YOUR_JWT_TOKEN"
```

#### Adım 3: Hints ve Explanation'ı Güncelle

```bash
curl -X PUT http://localhost:8080/api/questions/123/analysis \
  -H "Authorization: Bearer YOUR_JWT_TOKEN" \
  -H "Content-Type: application/json" \
  -d '{
    "hints": [
      "İlk ipucu: İşlem tablosunu dikkatlice inceleyin",
      "İkinci ipucu: b + c = 5 denklemini kullanın",
      "Üçüncü ipucu: a - b = c denklemini kullanın"
    ],
    "explanation": "Önce b + c = 5 denkleminden başlayarak, diğer denklemleri kullanarak a, b ve c değerlerini bulun. Sonra K, L ve M değerlerini hesaplayın ve K - L.M işlemini yapın."
  }'
```

**Beklenen Response:**

```json
{
  "success": true,
  "message": "Analysis updated successfully"
}
```

#### Adım 4: Güncellenmiş Soruyu Doğrula

```bash
curl -X GET http://localhost:8080/api/questions/123 \
  -H "Authorization: Bearer YOUR_JWT_TOKEN"
```

### Yöntem 3: Postman Collection

Postman'de yeni bir collection oluşturun ve şu request'leri ekleyin:

1. **POST** `/api/questions/add`

   - Headers: `Authorization: Bearer {{token}}`
   - Body: `form-data`
     - `file`: (file) question-image.png
     - `testId`: 1
     - `correctAnswer`: A
     - `answerNumber`: 5
     - `sequenceNumber`: 1

2. **GET** `/api/questions/{{questionId}}`

   - Headers: `Authorization: Bearer {{token}}`

3. **PUT** `/api/questions/{{questionId}}/analysis`
   - Headers:
     - `Authorization: Bearer {{token}}`
     - `Content-Type: application/json`
   - Body (raw JSON):
     ```json
     {
       "hints": [
         "İlk ipucu: İşlem tablosunu dikkatlice inceleyin",
         "İkinci ipucu: b + c = 5 denklemini kullanın",
         "Üçüncü ipucu: a - b = c denklemini kullanın"
       ],
       "explanation": "Önce b + c = 5 denkleminden başlayarak, diğer denklemleri kullanarak a, b ve c değerlerini bulun. Sonra K, L ve M değerlerini hesaplayın ve K - L.M işlemini yapın."
     }
     ```

## Test Kontrol Listesi

- [ ] Soru başarıyla yüklendi mi?
- [ ] AI analiz otomatik yapıldı mı?
- [ ] 3 hint otomatik eklendi mi?
- [ ] Explanation otomatik eklendi mi?
- [ ] Response'da hints ve explanation var mı?
- [ ] Hints ve explanation güncellenebiliyor mu?
- [ ] Güncelleme sonrası değişiklikler kaydedildi mi?
- [ ] Rate limiting çalışıyor mu? (10 req/min)
- [ ] Admin olmayan kullanıcı erişemiyor mu?

## Beklenen Hatalar ve Çözümleri

### Hata 1: 401 Unauthorized

**Sebep**: Geçersiz veya eksik JWT token
**Çözüm**: Login yaparak yeni token alın

### Hata 2: 403 Forbidden

**Sebep**: Kullanıcı admin değil
**Çözüm**: Admin rolüne sahip kullanıcı ile login yapın

### Hata 3: 400 Bad Request - "Test not found"

**Sebep**: Geçersiz testId
**Çözüm**: Mevcut bir testId kullanın

### Hata 4: 429 Too Many Requests

**Sebep**: Rate limit aşıldı (10 req/min)
**Çözüm**: 1 dakika bekleyin veya rate limit'i artırın

### Hata 5: AI Analiz Başarısız

**Sebep**: OpenAI API hatası veya görsel okunamadı
**Çözüm**:

- OpenAI API key'i kontrol edin
- Görsel dosyasının geçerli olduğundan emin olun
- Logları kontrol edin

## Log Kontrolü

Test sırasında logları kontrol etmek için:

```bash
# Application loglarını izle
tail -f logs/application.log

# Veya Spring Boot console output
# (Eğer IDE'den çalıştırıyorsanız console'da görünecek)
```

**Beklenen Log Mesajları:**

```
INFO  - Soru oluşturma başlatıldı: testId=1, correctAnswer=A
INFO  - Görsel kaydedildi: 1234567890_question-image.png
INFO  - Soru oluşturuldu: questionId=123
INFO  - AI analiz başlatıldı: questionId=123
INFO  - AI analiz tamamlandı: questionId=123
INFO  - 3 ipucu eklendi: questionId=123
INFO  - Çözüm eklendi: questionId=123
```

## Örnek Test Görseli

Test için kullanabileceğiniz örnek soru:

- Matematik işlem tablosu problemi (yukarıdaki görsel)
- Herhangi bir matematik sorusu görseli
- PNG, JPG, JPEG formatında

## Notlar

- AI analiz işlemi biraz zaman alabilir (10-30 saniye)
- Rate limiting: 10 request/dakika (USER-based)
- Sadece ADMIN rolü soru oluşturabilir
- Görsel dosyası `uploads/` klasörüne kaydedilir
- AI analiz sonuçları otomatik olarak hints ve explanation olarak eklenir
