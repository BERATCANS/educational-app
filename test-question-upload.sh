#!/bin/bash

# Test script for question upload with AI analysis
# Usage: ./test-question-upload.sh <JWT_TOKEN> <TEST_ID> <IMAGE_FILE_PATH>

# Colors for output
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
RED='\033[0;31m'
NC='\033[0m' # No Color

# Check arguments
if [ $# -lt 3 ]; then
    echo -e "${RED}Usage: ./test-question-upload.sh <JWT_TOKEN> <TEST_ID> <IMAGE_FILE_PATH>${NC}"
    echo -e "${YELLOW}Example: ./test-question-upload.sh 'eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...' 1 /path/to/question-image.png${NC}"
    exit 1
fi

JWT_TOKEN=$1
TEST_ID=$2
IMAGE_FILE=$3
BASE_URL="http://localhost:8080"

# Check if image file exists
if [ ! -f "$IMAGE_FILE" ]; then
    echo -e "${RED}Error: Image file not found: $IMAGE_FILE${NC}"
    exit 1
fi

echo -e "${GREEN}=== Testing Question Upload with AI Analysis ===${NC}"
echo ""
echo "Test ID: $TEST_ID"
echo "Image File: $IMAGE_FILE"
echo "Base URL: $BASE_URL"
echo ""

# Step 1: Upload question with AI analysis
echo -e "${YELLOW}Step 1: Uploading question with AI analysis...${NC}"
RESPONSE=$(curl -s -w "\n%{http_code}" -X POST "$BASE_URL/api/questions/add" \
  -H "Authorization: Bearer $JWT_TOKEN" \
  -F "file=@$IMAGE_FILE" \
  -F "testId=$TEST_ID" \
  -F "correctAnswer=A" \
  -F "answerNumber=5" \
  -F "sequenceNumber=1")

HTTP_CODE=$(echo "$RESPONSE" | tail -n1)
BODY=$(echo "$RESPONSE" | sed '$d')

if [ "$HTTP_CODE" -eq 200 ] || [ "$HTTP_CODE" -eq 201 ]; then
    echo -e "${GREEN}✓ Question uploaded successfully!${NC}"
    echo ""
    echo "Response:"
    echo "$BODY" | jq '.' 2>/dev/null || echo "$BODY"
    echo ""
    
    # Extract question ID from response
    QUESTION_ID=$(echo "$BODY" | jq -r '.id' 2>/dev/null)
    
    if [ "$QUESTION_ID" != "null" ] && [ -n "$QUESTION_ID" ]; then
        echo -e "${GREEN}Question ID: $QUESTION_ID${NC}"
        echo ""
        
        # Step 2: Get question details
        echo -e "${YELLOW}Step 2: Fetching question details...${NC}"
        QUESTION_RESPONSE=$(curl -s -w "\n%{http_code}" -X GET "$BASE_URL/api/questions/$QUESTION_ID" \
          -H "Authorization: Bearer $JWT_TOKEN")
        
        QUESTION_HTTP_CODE=$(echo "$QUESTION_RESPONSE" | tail -n1)
        QUESTION_BODY=$(echo "$QUESTION_RESPONSE" | sed '$d')
        
        if [ "$QUESTION_HTTP_CODE" -eq 200 ]; then
            echo -e "${GREEN}✓ Question details fetched!${NC}"
            echo ""
            echo "Question Details:"
            echo "$QUESTION_BODY" | jq '.' 2>/dev/null || echo "$QUESTION_BODY"
            echo ""
            
            # Check if hints and explanation are present
            HINTS_COUNT=$(echo "$QUESTION_BODY" | jq -r '.hints | length' 2>/dev/null)
            EXPLANATION=$(echo "$QUESTION_BODY" | jq -r '.explanation' 2>/dev/null)
            
            if [ "$HINTS_COUNT" -gt 0 ]; then
                echo -e "${GREEN}✓ Hints found: $HINTS_COUNT${NC}"
            else
                echo -e "${YELLOW}⚠ No hints found${NC}"
            fi
            
            if [ -n "$EXPLANATION" ] && [ "$EXPLANATION" != "null" ] && [ "$EXPLANATION" != "" ]; then
                echo -e "${GREEN}✓ Explanation found${NC}"
            else
                echo -e "${YELLOW}⚠ No explanation found${NC}"
            fi
            echo ""
            
            # Step 3: Test update analysis endpoint
            echo -e "${YELLOW}Step 3: Testing update analysis endpoint...${NC}"
            UPDATE_RESPONSE=$(curl -s -w "\n%{http_code}" -X PUT "$BASE_URL/api/questions/$QUESTION_ID/analysis" \
              -H "Authorization: Bearer $JWT_TOKEN" \
              -H "Content-Type: application/json" \
              -d '{
                "hints": [
                  "İlk ipucu: İşlem tablosunu dikkatlice inceleyin",
                  "İkinci ipucu: b + c = 5 denklemini kullanın",
                  "Üçüncü ipucu: a - b = c denklemini kullanın"
                ],
                "explanation": "Önce b + c = 5 denkleminden başlayarak, diğer denklemleri kullanarak a, b ve c değerlerini bulun. Sonra K, L ve M değerlerini hesaplayın ve K - L.M işlemini yapın."
              }')
            
            UPDATE_HTTP_CODE=$(echo "$UPDATE_RESPONSE" | tail -n1)
            UPDATE_BODY=$(echo "$UPDATE_RESPONSE" | sed '$d')
            
            if [ "$UPDATE_HTTP_CODE" -eq 200 ]; then
                echo -e "${GREEN}✓ Analysis updated successfully!${NC}"
                echo ""
                echo "Update Response:"
                echo "$UPDATE_BODY" | jq '.' 2>/dev/null || echo "$UPDATE_BODY"
                echo ""
                
                # Step 4: Verify updated question
                echo -e "${YELLOW}Step 4: Verifying updated question...${NC}"
                VERIFY_RESPONSE=$(curl -s -X GET "$BASE_URL/api/questions/$QUESTION_ID" \
                  -H "Authorization: Bearer $JWT_TOKEN")
                
                echo -e "${GREEN}✓ Updated Question Details:${NC}"
                echo "$VERIFY_RESPONSE" | jq '.' 2>/dev/null || echo "$VERIFY_RESPONSE"
            else
                echo -e "${RED}✗ Failed to update analysis. HTTP Code: $UPDATE_HTTP_CODE${NC}"
                echo "$UPDATE_BODY"
            fi
        else
            echo -e "${RED}✗ Failed to fetch question details. HTTP Code: $QUESTION_HTTP_CODE${NC}"
            echo "$QUESTION_BODY"
        fi
    else
        echo -e "${YELLOW}⚠ Could not extract question ID from response${NC}"
    fi
else
    echo -e "${RED}✗ Failed to upload question. HTTP Code: $HTTP_CODE${NC}"
    echo "$BODY"
    exit 1
fi

echo ""
echo -e "${GREEN}=== Test Complete ===${NC}"

