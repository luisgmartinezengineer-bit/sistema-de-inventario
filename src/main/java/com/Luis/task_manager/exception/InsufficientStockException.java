package com.Luis.task_manager.exception;

public class InsufficientStockException extends RuntimeException {
    public InsufficientStockException(String productName, int requested, int available) {
        super("Stock insuficiente para '%s': solicitado=%d, disponible=%d"
                .formatted(productName, requested, available));
    }
}
