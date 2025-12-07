package com.beratcan.app.exception;

public class PurchaseNotFoundException extends ResourceNotFoundException {
    public PurchaseNotFoundException(Long purchaseId) {
        super("Purchase not found with id: " + purchaseId);
    }
}
