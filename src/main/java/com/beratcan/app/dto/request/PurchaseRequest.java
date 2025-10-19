package com.beratcan.app.dto.request;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class PurchaseRequest {
    private String paymentMethod; // Ödeme yöntemi (isteğe bağlı)
    private String cardToken; // Ödeme gateway'inden gelecek token (ileride eklenebilir)
}
