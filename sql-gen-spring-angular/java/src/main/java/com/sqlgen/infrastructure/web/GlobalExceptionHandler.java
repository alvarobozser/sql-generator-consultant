package com.sqlgen.infrastructure.web;

import com.sqlgen.domain.exception.EmptyQuestionException;
import com.sqlgen.domain.exception.LlmException;
import com.sqlgen.domain.exception.SqlExecutionException;
import com.sqlgen.domain.exception.UnsafeSqlException;
import com.sqlgen.infrastructure.web.dto.ErrorResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataAccessException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.stream.Collectors;

/**
 * Manejador global de excepciones para los controllers REST.
 * Convierte excepciones de dominio a respuestas HTTP con codigos de estado
 * apropiados.
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler(EmptyQuestionException.class)
    public ResponseEntity<ErrorResponse> handleEmptyQuestion(EmptyQuestionException e) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
            .body(ErrorResponse.of(e.getMessage()));
    }

    @ExceptionHandler(UnsafeSqlException.class)
    public ResponseEntity<ErrorResponse> handleUnsafeSql(UnsafeSqlException e) {
        return ResponseEntity.status(HttpStatus.UNPROCESSABLE_ENTITY)
            .body(ErrorResponse.of(e.getMessage()));
    }

    /**
     * Error de BD: devolvemos 400 (la query fallo por datos, no por conexion).
     * Si la causa es un problema de conexion, lo detectamos y devolvemos 503.
     */
    @ExceptionHandler(SqlExecutionException.class)
    public ResponseEntity<ErrorResponse> handleSqlExecution(SqlExecutionException e) {
        if (e.getCause() instanceof DataAccessException
            || (e.getCause() != null && e.getCause().getClass().getName().contains("Connection"))) {
            log.error("BD no disponible", e);
            return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
                .body(ErrorResponse.of("Base de datos no disponible: " + e.getMessage()));
        }
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
            .body(ErrorResponse.of(e.getMessage()));
    }

    /**
     * Cualquier excepcion de acceso a datos no contemplada (DataAccessException):
     * suele ser problema de conexion con la BD -> 503 Service Unavailable.
     */
    @ExceptionHandler(DataAccessException.class)
    public ResponseEntity<ErrorResponse> handleDataAccess(DataAccessException e) {
        log.error("Error de acceso a datos", e);
        return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
            .body(ErrorResponse.of("Error de base de datos: " + e.getMessage()));
    }

    @ExceptionHandler(LlmException.class)
    public ResponseEntity<ErrorResponse> handleLlm(LlmException e) {
        log.error("Error del LLM", e);
        return ResponseEntity.status(HttpStatus.BAD_GATEWAY)
            .body(ErrorResponse.of("Error con el proveedor de IA: " + e.getMessage()));
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleValidation(MethodArgumentNotValidException e) {
        String details = e.getBindingResult().getFieldErrors().stream()
            .map(fe -> fe.getField() + ": " + fe.getDefaultMessage())
            .collect(Collectors.joining("; "));
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
            .body(new ErrorResponse("Errores de validacion", java.util.Map.of("fields", details)));
    }
}
