#!/bin/bash
# Schema Comparison Script
# Mevcut database schema'sı ile V1__Initial_schema.sql'i karşılaştırır

echo "============================================"
echo "SCHEMA KARŞILAŞTIRMA"
echo "============================================"
echo ""

# Mevcut database schema'sını export et (eğer pg_dump varsa)
CURRENT_SCHEMA_FILE="/tmp/current_schema.sql"
MIGRATION_SCHEMA_FILE="src/main/resources/db/migration/V1__Initial_schema.sql"

echo "1. Mevcut database schema'sını export ediliyor..."
if command -v pg_dump &> /dev/null; then
    pg_dump -h localhost -U berat -d soruapp \
      --schema-only \
      --no-owner \
      --no-privileges \
      --no-tablespaces \
      --clean \
      --if-exists \
      > "$CURRENT_SCHEMA_FILE" 2>/dev/null
    
    if [ $? -eq 0 ]; then
        echo "   ✓ Schema export edildi: $CURRENT_SCHEMA_FILE"
    else
        echo "   ✗ Schema export edilemedi. Lütfen manuel olarak export edin."
        echo "   IntelliJ IDEA Database tool kullanabilirsiniz."
        exit 1
    fi
else
    echo "   ⚠ pg_dump bulunamadı."
    echo "   Lütfen mevcut schema'yı manuel olarak export edin:"
    echo "   - IntelliJ IDEA: Database tool -> Export to File -> SQL"
    echo "   - veya pgAdmin/DBeaver kullanın"
    echo ""
    echo "   Export edilen dosyayı şuraya koyun: $CURRENT_SCHEMA_FILE"
    read -p "   Devam etmek için Enter'a basın..."
fi

echo ""
echo "2. Migration schema dosyası kontrol ediliyor..."
if [ ! -f "$MIGRATION_SCHEMA_FILE" ]; then
    echo "   ✗ Migration schema dosyası bulunamadı: $MIGRATION_SCHEMA_FILE"
    exit 1
fi
echo "   ✓ Migration schema dosyası bulundu"

echo ""
echo "3. Tablolar karşılaştırılıyor..."
echo ""

# Mevcut schema'daki tabloları çıkar
if [ -f "$CURRENT_SCHEMA_FILE" ]; then
    CURRENT_TABLES=$(grep -i "CREATE TABLE" "$CURRENT_SCHEMA_FILE" | sed 's/CREATE TABLE IF NOT EXISTS //i' | sed 's/CREATE TABLE //i' | sed 's/ (.*//' | tr -d ' ' | sort)
    MIGRATION_TABLES=$(grep -i "CREATE TABLE" "$MIGRATION_SCHEMA_FILE" | sed 's/CREATE TABLE IF NOT EXISTS //i' | sed 's/CREATE TABLE //i' | sed 's/ (.*//' | tr -d ' ' | sort)
    
    echo "MEVCUT SCHEMA'DAKİ TABLOLAR:"
    echo "$CURRENT_TABLES" | nl
    echo ""
    echo "MIGRATION SCHEMA'DAKİ TABLOLAR:"
    echo "$MIGRATION_TABLES" | nl
    echo ""
    
    # Eksik tablolar
    MISSING_IN_MIGRATION=$(comm -23 <(echo "$CURRENT_TABLES") <(echo "$MIGRATION_TABLES"))
    EXTRA_IN_MIGRATION=$(comm -13 <(echo "$CURRENT_TABLES") <(echo "$MIGRATION_TABLES"))
    
    if [ ! -z "$MISSING_IN_MIGRATION" ]; then
        echo "⚠ MIGRATION'DA EKSİK TABLOLAR (Mevcut schema'da var ama migration'da yok):"
        echo "$MISSING_IN_MIGRATION" | nl
        echo ""
    fi
    
    if [ ! -z "$EXTRA_IN_MIGRATION" ]; then
        echo "⚠ MIGRATION'DA FAZLALIK TABLOLAR (Migration'da var ama mevcut schema'da yok):"
        echo "$EXTRA_IN_MIGRATION" | nl
        echo ""
    fi
    
    if [ -z "$MISSING_IN_MIGRATION" ] && [ -z "$EXTRA_IN_MIGRATION" ]; then
        echo "✓ Tüm tablolar eşleşiyor!"
    fi
fi

echo ""
echo "4. Index'ler karşılaştırılıyor..."
if [ -f "$CURRENT_SCHEMA_FILE" ]; then
    CURRENT_INDEXES=$(grep -i "CREATE INDEX" "$CURRENT_SCHEMA_FILE" | sed 's/CREATE INDEX IF NOT EXISTS //i' | sed 's/CREATE INDEX //i' | sed 's/ ON .*//' | tr -d ' ' | sort)
    MIGRATION_INDEXES=$(grep -i "CREATE INDEX" "$MIGRATION_SCHEMA_FILE" | sed 's/CREATE INDEX IF NOT EXISTS //i' | sed 's/CREATE INDEX //i' | sed 's/ ON .*//' | tr -d ' ' | sort)
    
    MISSING_INDEXES=$(comm -23 <(echo "$CURRENT_INDEXES") <(echo "$MIGRATION_INDEXES"))
    EXTRA_INDEXES=$(comm -13 <(echo "$CURRENT_INDEXES") <(echo "$MIGRATION_INDEXES"))
    
    if [ ! -z "$MISSING_INDEXES" ]; then
        echo "⚠ MIGRATION'DA EKSİK INDEX'LER:"
        echo "$MISSING_INDEXES" | nl
    fi
    
    if [ ! -z "$EXTRA_INDEXES" ]; then
        echo "⚠ MIGRATION'DA FAZLALIK INDEX'LER:"
        echo "$EXTRA_INDEXES" | nl
    fi
    
    if [ -z "$MISSING_INDEXES" ] && [ -z "$EXTRA_INDEXES" ]; then
        echo "✓ Tüm index'ler eşleşiyor!"
    fi
fi

echo ""
echo "============================================"
echo "KARŞILAŞTIRMA TAMAMLANDI"
echo "============================================"
echo ""
echo "Detaylı karşılaştırma için:"
echo "  diff $CURRENT_SCHEMA_FILE $MIGRATION_SCHEMA_FILE"
