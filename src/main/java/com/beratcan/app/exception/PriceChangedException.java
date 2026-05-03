package com.beratcan.app.exception;

import lombok.Getter;

import java.math.BigDecimal;
import java.util.List;

/**
 * Checkout sırasında bir veya daha fazla item'ın güncel fiyatı sepete
 * eklendiği andaki fiyattan farklıysa fırlatılır. Handler 409 Conflict
 * + değişen item listesi döner; client kullanıcıya yeni toplamı onaylatıp
 * /api/cart/refresh-prices ile sepetini günceller, sonra checkout'u tekrarlar.
 */
@Getter
public class PriceChangedException extends RuntimeException {

    private final List<ChangedItem> changedItems;

    public PriceChangedException(List<ChangedItem> changedItems) {
        super("Sepetinizdeki bazı kitapların fiyatı değişti. Lütfen yeni toplamı onaylayın.");
        this.changedItems = changedItems;
    }

    @Getter
    public static class ChangedItem {
        private final Long cartItemId;
        private final Long bookId;
        private final String bookTitle;
        private final BigDecimal oldPrice;
        private final BigDecimal newPrice;

        public ChangedItem(Long cartItemId, Long bookId, String bookTitle,
                           BigDecimal oldPrice, BigDecimal newPrice) {
            this.cartItemId = cartItemId;
            this.bookId = bookId;
            this.bookTitle = bookTitle;
            this.oldPrice = oldPrice;
            this.newPrice = newPrice;
        }
    }
}
