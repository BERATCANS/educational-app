package com.beratcan.app.dto.response;

import com.beratcan.app.entity.CartItem;
import com.beratcan.app.entity.DiscountCode;
import lombok.Data;

import java.math.BigDecimal;
import java.util.List;

@Data
public class DiscountValidationResult {
    private boolean valid;
    private String errorMessage;
    private DiscountCode discountCode;
    private BigDecimal discountAmount;
    private List<CartItem> applicableItems;

    public static DiscountValidationResult valid(
            DiscountCode code,
            BigDecimal amount,
            List<CartItem> items) {
        DiscountValidationResult result = new DiscountValidationResult();
        result.valid = true;
        result.discountCode = code;
        result.discountAmount = amount;
        result.applicableItems = items;
        return result;
    }

    public static DiscountValidationResult invalid(String message) {
        DiscountValidationResult result = new DiscountValidationResult();
        result.valid = false;
        result.errorMessage = message;
        return result;
    }
}
