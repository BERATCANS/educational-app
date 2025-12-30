# AI Chat Streaming Implementation

## Genel Bakış

AI chatbot'tan gelen yanıtları chunk chunk alıp Server-Sent Events (SSE) ile frontend'e real-time olarak stream etme implementasyonu tamamlandı.

## Değişiklik Yapılan Dosyalar

### 1. QuestionAIChatController.java

**Konum:** `src/main/java/com/beratcan/app/controller/QuestionAIChatController.java`

**Değişiklikler:**

- ✅ `SseEmitter` import eklendi
- ✅ Yeni endpoint eklendi: `POST /api/questions/{questionId}/ai/chat/messages/stream`
- ✅ Rate limiting: 20 request/minute
- ✅ Timeout: 30 saniye

### 2. QuestionAIChatService.java

**Konum:** `src/main/java/com/beratcan/app/service/QuestionAIChatService.java`

**Değişiklikler:**

- ✅ Import'lar eklendi: `SseEmitter`, `BufferedReader`, `InputStreamReader`, `@Async`
- ✅ `sendMessageStream()` metodu eklendi (@Async)
- ✅ `callOpenAIChatAPIStream()` metodu eklendi
- ✅ Helper metodlar eklendi:
  - `buildMessagesForAPI()` - OpenAI API için message listesi oluşturur
  - `prepareQuestionImage()` - Soru görselini base64'e encode eder
  - `detectImageMimeType()` - Görsel MIME type'ını tespit eder

## API Kullanımı

### Endpoint

```
POST /api/questions/{questionId}/ai/chat/messages/stream
```

### Headers

```
Content-Type: application/json
Authorization: Bearer {token}
```

### Request Body

```json
{
  "message": "Bu soruyu nasıl çözerim?"
}
```

### Response (SSE Stream)

**Chunk Event:**

```
data: {"type":"chunk","content":"Bu "}
data: {"type":"chunk","content":"soruyu "}
data: {"type":"chunk","content":"çözmek için..."}
```

**Done Event:**

```
data: {"type":"done","messageId":123}
```

## Frontend Entegrasyonu

### JavaScript/React Örneği

```javascript
const sendStreamingMessage = async (questionId, message) => {
  const response = await fetch(
    `/api/questions/${questionId}/ai/chat/messages/stream`,
    {
      method: "POST",
      headers: {
        "Content-Type": "application/json",
        Authorization: `Bearer ${token}`,
      },
      body: JSON.stringify({ message }),
    }
  );

  const reader = response.body.getReader();
  const decoder = new TextDecoder();

  let aiMessage = "";

  while (true) {
    const { done, value } = await reader.read();
    if (done) break;

    const chunk = decoder.decode(value);
    const lines = chunk.split("\n");

    for (const line of lines) {
      if (line.startsWith("data: ")) {
        const data = JSON.parse(line.substring(6));

        if (data.type === "chunk") {
          aiMessage += data.content;
          updateChatUI(aiMessage); // UI'ı güncelle
        } else if (data.type === "done") {
          console.log("Stream tamamlandı, messageId:", data.messageId);
          markMessageAsComplete(data.messageId);
        }
      }
    }
  }
};
```

## Test

### HTML Test Client

**Konum:** `docs/ai-chat-streaming-test.html`

Test için:

1. Browser'da `docs/ai-chat-streaming-test.html` dosyasını açın
2. Base URL (örn: `http://localhost:8080`) girin
3. Bearer token'ınızı girin
4. Question ID girin
5. Mesaj yazıp gönder'e basın
6. AI yanıtının chunk chunk geldiğini görün

## Teknik Detaylar

### OpenAI API

- **Endpoint:** `https://api.openai.com/v1/chat/completions`
- **Stream:** `true` parametresi eklendi
- **Response Format:** Server-Sent Events (SSE)
- **Chunk Format:** `data: {JSON}\n\n`

### Spring Boot

- **SseEmitter:** 30 saniye timeout
- **@Async:** `aiAnalysisExecutor` thread pool kullanılıyor
- **Error Handling:** `emitter.completeWithError()` ile hata iletimi
- **DB Kayıt:** Stream bittikten sonra tam yanıt kaydediliyor

### Akış Diyagramı

```
Frontend → Controller → Service → OpenAI API
                                      ↓
                                   Stream chunks
                                      ↓
Service ← ← ← ← ← ← ← ← ← ← ← ← ← ← ← ←
   ↓
SSE Event (chunk)
   ↓
Controller
   ↓
Frontend (UI güncelle)
```

## Avantajlar

1. ✅ **Daha iyi UX:** Kullanıcı AI'ın düşündüğünü gerçek zamanlı görür
2. ✅ **Algılanan performans:** Bekleme süresi daha kısa hissedilir
3. ✅ **Backward compatible:** Mevcut `/messages` endpoint aynen çalışıyor
4. ✅ **Maliyet değişmez:** Aynı OpenAI API, sadece streaming mode

## Dikkat Edilmesi Gerekenler

1. **Timeout:** SseEmitter 30 saniye timeout (uzun yanıtlar için yeterli)
2. **Error Handling:** Stream sırasında hata olursa `emitter.completeWithError()` çağrılır
3. **Connection Close:** Frontend bağlantıyı kapatırsa backend exception alır
4. **Async Execution:** @Async ile çalışıyor, thread pool yönetimi önemli
5. **DB Kayıt:** Tam yanıt stream bittikten sonra kaydediliyor

## Sorun Giderme

### Problem: Stream başlamıyor

- Token'ın geçerli olduğunu kontrol edin
- Question ID'nin doğru olduğunu kontrol edin
- Rate limiting kontrolü yapın (20 req/min)

### Problem: Chunk'lar gelmiyor

- OpenAI API key'in geçerli olduğunu kontrol edin
- Network bağlantısını kontrol edin
- Browser console'da hata var mı kontrol edin

### Problem: Stream yarıda kesiliyor

- Timeout süresini artırın (SseEmitter constructor)
- Network stabilitysi kontrol edin
- Backend log'larını inceleyin

## Log Örnekleri

**Başarılı Stream:**

```
2025-12-29 15:30:45 - INFO  - OpenAI streaming completed: duration=3245ms, responseLength=482
```

**Hata:**

```
2025-12-29 15:31:12 - ERROR - Stream error: questionId=123, userId=456, error=OpenAI API error: 401
```

## Sonraki Adımlar

1. ✅ Backend implementation tamamlandı
2. ⏳ Frontend'de production implementasyonu
3. ⏳ Load testing (concurrent streaming)
4. ⏳ Monitoring ve metrics ekleme

## Notlar

- Mevcut `/messages` endpoint normal mode'da çalışmaya devam ediyor
- Frontend'de streaming veya normal mode seçilebilir
- Streaming sadece text-to-text chat için, voice için ayrı implementasyon gerekli
