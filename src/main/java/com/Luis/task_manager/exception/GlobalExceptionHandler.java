package com.Luis.task_manager.exception;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Manejador global de excepciones para todos los controladores REST.
 *
 * <p>Intercepta las excepciones lanzadas en cualquier parte de la capa de servicio
 * o controladores y las convierte en respuestas HTTP con formato JSON estructurado
 * que incluye: timestamp, código de estado, descripción del error y mensaje.</p>
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    /**
     * Maneja el caso en que se solicita un recurso que no existe en la base de datos.
     * Retorna HTTP 404 Not Found.
     */
    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<Map<String, Object>> handleNotFound(ResourceNotFoundException ex) {
        return error(HttpStatus.NOT_FOUND, ex.getMessage());
    }

    /**
     * Maneja el intento de vender más unidades de las disponibles en stock.
     * Retorna HTTP 409 Conflict para indicar un conflicto con el estado actual del recurso.
     */
    @ExceptionHandler(InsufficientStockException.class)
    public ResponseEntity<Map<String, Object>> handleInsufficientStock(InsufficientStockException ex) {
        return error(HttpStatus.CONFLICT, ex.getMessage());
    }

    /**
     * Maneja errores de lógica de negocio como tipos de movimiento inválidos
     * o stock insuficiente en ajustes manuales. Retorna HTTP 400 Bad Request.
     */
    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<Map<String, Object>> handleIllegalArgument(IllegalArgumentException ex) {
        return error(HttpStatus.BAD_REQUEST, ex.getMessage());
    }

    /**
     * Maneja los errores de validación de Jakarta Validation (@NotBlank, @Min, @Size, etc.).
     * Concatena todos los mensajes de error de campo en una sola respuesta HTTP 400.
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Map<String, Object>> handleValidation(MethodArgumentNotValidException ex) {
        String message = ex.getBindingResult().getFieldErrors().stream()
                .map(e -> e.getField() + ": " + e.getDefaultMessage())
                .collect(Collectors.joining(", "));
        return error(HttpStatus.BAD_REQUEST, message);
    }

    /**
     * Construye el cuerpo de respuesta de error estándar del API.
     *
     * @param status  código de estado HTTP
     * @param message mensaje descriptivo del error
     */
    private ResponseEntity<Map<String, Object>> error(HttpStatus status, String message) {
        Map<String, Object> body = new HashMap<>();
        body.put("timestamp", LocalDateTime.now());
        body.put("status", status.value());
        body.put("error", status.getReasonPhrase());
        body.put("message", message);
        return ResponseEntity.status(status).body(body);
    }
}
