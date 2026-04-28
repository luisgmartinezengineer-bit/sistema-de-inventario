package com.Luis.task_manager.exception;

/**
 * Excepción lanzada cuando se intenta acceder a un recurso que no existe
 * en la base de datos (producto, venta, categoría, etc.).
 *
 * <p>Es manejada por {@link GlobalExceptionHandler} y retorna HTTP 404 Not Found.</p>
 */
public class ResourceNotFoundException extends RuntimeException {

    /**
     * @param message descripción del recurso no encontrado (ej. "Producto no encontrado: 42")
     */
    public ResourceNotFoundException(String message) {
        super(message);
    }
}
