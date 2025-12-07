# Environment Variables Setup Guide

## 🔒 Güvenlik İçin EMAIL_PASSWORD Ayarlama

Email şifresini güvenli bir şekilde saklamak için aşağıdaki yöntemlerden birini kullanın:

---

## ✅ Yöntem 1: Environment Variable (ÖNERİLEN - En Güvenli)

### macOS/Linux:

```bash
# Terminal'de (kalıcı olması için ~/.zshrc veya ~/.bashrc'ye ekleyin)
export EMAIL_PASSWORD="your-hostinger-email-password"

# Veya sadece bu oturum için:
export EMAIL_PASSWORD="your-hostinger-email-password"
```

### Windows (PowerShell):

```powershell
$env:EMAIL_PASSWORD="your-hostinger-email-password"
```

### Windows (CMD):

```cmd
set EMAIL_PASSWORD=your-hostinger-email-password
```

### IntelliJ IDEA'da:

1. Run → Edit Configurations
2. Environment variables bölümüne: `EMAIL_PASSWORD=your-password`
3. Apply → OK

---

## ✅ Yöntem 2: .env Dosyası (Development için)

### Adımlar:

1. Proje root dizininde `.env` dosyası oluşturun:

   ```bash
   touch .env
   ```

2. `.env` dosyasına şunu ekleyin:

   ```
   EMAIL_PASSWORD=your-hostinger-email-password
   ```

3. `.env` dosyası `.gitignore`'da olduğu için Git'e commit edilmeyecek ✅

### Not:

- Spring Boot 3.x `.env` dosyasını otomatik okumaz
- `dotenv-java` dependency eklemeniz gerekir (opsiyonel)
- Veya IDE'niz `.env` dosyasını otomatik yükleyebilir

---

## ✅ Yöntem 3: System Properties (Java ile)

Uygulamayı çalıştırırken:

```bash
java -DEMAIL_PASSWORD=your-password -jar app.jar
```

Maven ile:

```bash
mvn spring-boot:run -Dspring-boot.run.arguments="--EMAIL_PASSWORD=your-password"
```

---

## 🚀 Production Ortamı İçin

### Docker:

```yaml
# docker-compose.yml
services:
  app:
    environment:
      - EMAIL_PASSWORD=${EMAIL_PASSWORD}
```

Veya Docker secrets kullanın:

```yaml
secrets:
  email_password:
    external: true
```

### Cloud Platforms:

#### AWS (Elastic Beanstalk / ECS):

- Environment Variables → `EMAIL_PASSWORD` ekleyin
- Veya AWS Secrets Manager kullanın

#### Heroku:

```bash
heroku config:set EMAIL_PASSWORD=your-password
```

#### Railway / Render:

- Dashboard'dan Environment Variables ekleyin

---

## ⚠️ GÜVENLİK UYARILARI

### ❌ YAPMAYIN:

- `application.properties` içine şifreyi yazmayın
- Git'e commit etmeyin
- Log dosyalarında yazdırmayın
- Kod içinde hardcode etmeyin

### ✅ YAPIN:

- Environment variable kullanın
- `.env` dosyasını `.gitignore`'a ekleyin (✅ zaten eklendi)
- Production'da secrets management kullanın
- Şifreleri düzenli olarak rotate edin

---

## 🧪 Test Etme

Şifrenin doğru yüklendiğini kontrol etmek için:

```bash
# Terminal'de
echo $EMAIL_PASSWORD

# Veya uygulamayı başlatıp email göndermeyi deneyin
```

---

## 📝 Diğer Hassas Bilgiler

Aynı yöntemleri şunlar için de kullanabilirsiniz:

- `JWT_SECRET` (zaten environment variable olarak ayarlanmış)
- `DATABASE_PASSWORD` (ileride eklenebilir)
- `OPENAI_API_KEY` (şu anda hardcode - düzeltilmeli)

---

## 🔧 Mevcut Durum

`application.properties` dosyasında:

```properties
spring.mail.password=${EMAIL_PASSWORD:your-hostinger-email-password}
```

Bu şu anlama gelir:

- Önce `EMAIL_PASSWORD` environment variable'ını kontrol eder
- Bulamazsa default değeri kullanır (sadece development için)

**Production'da mutlaka environment variable kullanın!**
