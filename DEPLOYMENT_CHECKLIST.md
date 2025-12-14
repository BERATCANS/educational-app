# DigitalOcean Deployment Checklist

## ✅ Kod Hazırlığı (Tamamlandı)

### 1. Production Profile

- [x] `application-prod.properties` oluşturuldu
- [x] `ddl-auto=validate` ayarlandı
- [x] Connection pool ayarları eklendi
- [x] Logging seviyesi INFO'ya ayarlandı

### 2. Cloud Storage Entegrasyonu

- [x] StorageService interface oluşturuldu
- [x] LocalStorageService (@Profile("dev")) oluşturuldu
- [x] CloudStorageService (@Profile("prod")) oluşturuldu
- [x] AWS SDK v2 dependency eklendi
- [x] FileService refactor edildi
- [x] QuestionService ve QuestionAIChatService güncellendi

### 3. Security İyileştirmeleri

- [x] Swagger UI production'da admin-only yapıldı
- [x] CORS headers spesifik hale getirildi
- [x] JWT secret validation eklendi

### 4. Database Migration

- [x] Flyway dependencies eklendi
- [x] `V1__Initial_schema.sql` oluşturuldu (entity'lere göre)
- [x] Flyway configuration eklendi

### 5. Health Check

- [x] Actuator dependency eklendi
- [x] Health check configuration eklendi

### 6. Environment Variables

- [x] `.env.example` güncellendi
- [x] `application.properties` güncellendi

---

## 🔴 DigitalOcean'da Yapılacaklar

### 1. PostgreSQL Managed Database Oluşturma

1. DigitalOcean Dashboard → Databases → Create Database
2. **Database Engine:** PostgreSQL
3. **Version:** 16 (veya mevcut versiyon)
4. **Datacenter Region:** Seçin (örn: NYC3)
5. **Database Name:** `soruapp` (veya istediğiniz isim)
6. **User:** Oluşturulan user'ı not edin
7. **Password:** Güçlü bir şifre oluşturun

**Not:** Connection string'i not edin:

```
Host: db-postgresql-nyc3-xxxxx.db.ondigitalocean.com
Port: 25060
Database: soruapp
User: doadmin
Password: [oluşturduğunuz şifre]
```

### 2. Spaces (Object Storage) Oluşturma

1. DigitalOcean Dashboard → Spaces → Create a Space
2. **Datacenter Region:** Database ile aynı region (NYC3 önerilir)
3. **Space Name:** `soruapp-files` (veya istediğiniz isim)
4. **CDN Endpoint:** Enable edin (opsiyonel ama önerilir)
5. **File Listing:** Disable (güvenlik için)

**Spaces Credentials:**

1. API → Tokens/Keys → Spaces access keys
2. Generate New Key
3. **Key Name:** `soruapp-spaces-key`
4. Access Key ve Secret Key'i not edin

**Bucket Settings:**

- **Bucket Name:** Not edin
- **Region:** Not edin (örn: nyc3)
- **Endpoint:** `nyc3.digitaloceanspaces.com`
- **CDN Endpoint:** `soruapp-files.nyc3.cdn.digitaloceanspaces.com` (eğer CDN aktifse)

### 3. App Platform Uygulaması Oluşturma

1. DigitalOcean Dashboard → App Platform → Create App
2. **Source:** GitHub repository'nizi bağlayın
3. **Branch:** `main` veya production branch
4. **Build Command:** `./mvnw clean package -DskipTests`
5. **Run Command:** `java -jar target/app-0.0.1-SNAPSHOT.jar --spring.profiles.active=prod`

**Environment Variables Ekle:**

```bash
# Database
DB_URL=jdbc:postgresql://[HOST]:[PORT]/soruapp?sslmode=require
DB_USERNAME=[USERNAME]
DB_PASSWORD=[PASSWORD]

# JWT
JWT_SECRET=[min-32-karakter-guclu-random-string]

# OpenAI
OPENAI_API_KEY=[your-openai-key]
OPENAI_MODEL=gpt-4o
OPENAI_TIMEOUT=120

# Email
EMAIL_HOST=smtp.hostinger.com
EMAIL_PORT=465
EMAIL_USERNAME=[your-email]
EMAIL_PASSWORD=[your-password]
APP_EMAIL_FROM=[your-email]

# CORS
ALLOWED_ORIGINS=https://yourdomain.com,https://www.yourdomain.com

# Application
APP_BASE_URL=https://your-app-url.com
SPRING_PROFILES_ACTIVE=prod

# DigitalOcean Spaces
DO_SPACES_ENDPOINT=nyc3.digitaloceanspaces.com
DO_SPACES_KEY=[spaces-access-key]
DO_SPACES_SECRET=[spaces-secret-key]
DO_SPACES_BUCKET=[bucket-name]
DO_SPACES_REGION=nyc3
DO_SPACES_CDN_ENDPOINT=https://[bucket-name].nyc3.cdn.digitaloceanspaces.com
```

**Resource Settings:**

- **Plan:** Basic ($5/month) veya Professional
- **Instance Size:** 512MB RAM minimum (1GB önerilir)
- **Instance Count:** 1 (başlangıç için)

### 4. Database Connection (SSL)

PostgreSQL Managed Database SSL gerektirir. Connection string'e `?sslmode=require` ekleyin:

```
jdbc:postgresql://host:port/database?sslmode=require
```

### 5. Spaces Bucket Permissions

Spaces bucket'ınızda:

1. **Settings → File Listing:** Disable
2. **Settings → CORS Configurations:**
   - Allowed Origins: `https://your-app-url.com`
   - Allowed Methods: `GET, PUT, POST, DELETE, HEAD`
   - Allowed Headers: `*`
   - Max Age: `3600`

### 6. First Deployment

1. App Platform → Deploy
2. Build log'larını kontrol edin
3. Runtime log'larını kontrol edin
4. Health check endpoint'i test edin: `https://your-app-url.com/actuator/health`

---

## ⚠️ Önemli Kontroller

### Deployment Öncesi

- [ ] Tüm environment variables App Platform'da ayarlandı
- [ ] JWT_SECRET minimum 32 karakter
- [ ] Database connection string SSL ile (`?sslmode=require`)
- [ ] Spaces credentials doğru
- [ ] CORS origins production URL'lerini içeriyor
- [ ] `SPRING_PROFILES_ACTIVE=prod` ayarlandı

### Deployment Sonrası

- [ ] Health check çalışıyor: `/actuator/health`
- [ ] Database bağlantısı başarılı
- [ ] File upload test edildi (Spaces'e yükleniyor mu?)
- [ ] File download test edildi (Spaces'ten okunuyor mu?)
- [ ] Swagger UI production'da kapalı (admin-only)
- [ ] Error messages generic (detaylı değil)
- [ ] Logging seviyesi INFO

---

## 🐛 Sorun Giderme

### Database Connection Hatası

- SSL mode kontrol edin: `?sslmode=require`
- Firewall rules kontrol edin (App Platform IP'lerine izin verilmeli)
- Database user permissions kontrol edin

### Spaces Connection Hatası

- Access Key ve Secret Key doğru mu?
- Endpoint format doğru mu? (`nyc3.digitaloceanspaces.com`)
- Region doğru mu?

### File Upload Hatası

- Spaces bucket permissions kontrol edin
- CORS configuration kontrol edin
- CDN endpoint doğru mu?

### Application Başlamıyor

- Environment variables eksik mi?
- JWT_SECRET 32 karakterden kısa mı?
- Database connection başarılı mı?
- Log'ları kontrol edin: App Platform → Runtime Logs

---

## 📝 Notlar

1. **Flyway Migration:** İlk deployment'ta Flyway mevcut schema'yı baseline olarak işaretleyecek. Eğer database boşsa, migration çalışacak.

2. **File Storage:** Production'da `SPRING_PROFILES_ACTIVE=prod` olduğunda CloudStorageService aktif olacak.

3. **Database SSL:** Managed Database SSL zorunlu. Connection string'e `?sslmode=require` ekleyin.

4. **Spaces CDN:** CDN endpoint kullanmak performans için önerilir ama zorunlu değil.

5. **Health Check:** `/actuator/health` endpoint'i public erişilebilir olmalı (monitoring için).

---

## ✅ Deployment Sonrası Test Listesi

1. [ ] Health check: `GET /actuator/health`
2. [ ] User registration: `POST /api/users/register`
3. [ ] User login: `POST /api/users/login`
4. [ ] File upload: `POST /api/files/upload` (admin)
5. [ ] File download: `GET /api/files/{filename}`
6. [ ] Swagger UI erişimi: Production'da admin-only olmalı
7. [ ] Error handling: Generic mesajlar dönüyor mu?

---

**Hazır! 🚀**

Kod tarafı tamamlandı. DigitalOcean'da yukarıdaki adımları takip ederek deployment yapabilirsiniz.
