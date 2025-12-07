package com.beratcan.app.entity.enums;

public enum PurchaseStatus {
    PENDING, // Ödeme beklemede (henüz ödeme yapılmadı)
    COMPLETED, // Satın alma tamamlandı (ödeme başarılı, kitap kütüphaneye eklendi)
    FAILED, // Ödeme başarısız
    CANCELLED, // İptal edildi (kullanıcı veya sistem tarafından)
    REFUNDED // İade edildi (para iadesi yapıldı)
}
