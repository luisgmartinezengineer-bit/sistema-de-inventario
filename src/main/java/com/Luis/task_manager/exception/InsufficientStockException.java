package com.Luis.task_manager.exception;

/**
 * Excepción lanzada cuando se intenta vender o retirar más unidades
 * de las que hay disponibles en el inventario para un producto.
 *
 * <p>Es manejada por {@link GlobalExceptionHandler} y retorna HTTP 409 Conflict.</p>
 */
public class InsufficientStockException extends RuntimeException {

    /**
     * @param productName nombre del producto con stock insuficiente
     * @param requested   cantidad solicitada en la venta
     * @param available   cantidad disponible actualmente en el inventario
     */
    public InsufficientStockException(String productName, int requested, int available) {
        super("Stock insuficiente para '%s': solicitado=%d, disponible=%d"
                .formatted(productName, requested, available));
    }
}
