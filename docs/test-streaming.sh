#!/bin/bash

# AI Chat Streaming Test Script
# Kullanım: ./test-streaming.sh

BASE_URL="http://localhost:8080"
QUESTION_ID=1
MESSAGE="Bu soruyu nasıl çözerim?"

echo "🔐 Login yapılıyor..."
LOGIN_RESPONSE=$(curl -s -X POST "$BASE_URL/api/users/login" \
  -H "Content-Type: application/json" \
  -d '{"identifier":"admin2","password":"admin123"}')

TOKEN=$(echo $LOGIN_RESPONSE | jq -r '.accessToken')

if [ "$TOKEN" == "null" ] || [ -z "$TOKEN" ]; then
  echo "❌ Login başarısız!"
  echo "Response: $LOGIN_RESPONSE"
  exit 1
fi

echo "✅ Login başarılı!"
echo "📤 Streaming request gönderiliyor..."
echo ""

curl -N -X POST "$BASE_URL/api/questions/$QUESTION_ID/ai/chat/messages/stream" \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer $TOKEN" \
  -d "{\"message\":\"$MESSAGE\"}" \
  2>/dev/null | while IFS= read -r line; do
    if [[ $line == data:* ]]; then
      json_data="${line#data: }"
      echo "$json_data" | jq -r '.content // .type // empty' 2>/dev/null
    fi
  done

echo ""
echo "✅ Test tamamlandı!"

