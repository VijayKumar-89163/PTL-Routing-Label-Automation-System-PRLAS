package com.PRLAS.exception;

import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataAccessException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.multipart.MaxUploadSizeExceededException;
import org.springframework.web.multipart.MultipartException;
import org.springframework.web.servlet.NoHandlerFoundException;
import org.springframework.web.servlet.resource.NoResourceFoundException;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

@ControllerAdvice
@Slf4j
public class GlobalExceptionHandler {

    // Helper method for uniform response structure
    private ResponseEntity<Map<String, Object>> buildResponse(HttpStatus status, String errorCode, String message) {
        Map<String, Object> body = new HashMap<>();
        body.put("timestamp", LocalDateTime.now().toString());
        body.put("status", status.value());
        body.put("error", errorCode);
        body.put("message", message);
        return new ResponseEntity<>(body, status);
    }

    // 1. Custom Business Exceptions: LPN Not Found
    @ExceptionHandler(LpnNotFoundException.class)
    public ResponseEntity<Map<String, Object>> handleLpnNotFound(LpnNotFoundException ex) {
        log.warn("LPN Lookup Error: {}", ex.getMessage());
        return buildResponse(HttpStatus.NOT_FOUND, "LPN_NOT_FOUND", ex.getMessage());
    }

    // 2. Custom Business Exceptions: Invalid CSV File
    @ExceptionHandler(InvalidCsvException.class)
    public ResponseEntity<Map<String, Object>> handleInvalidCsv(InvalidCsvException ex) {
        log.error("CSV Import Error: {}", ex.getMessage());
        return buildResponse(HttpStatus.BAD_REQUEST, "INVALID_CSV_FILE", ex.getMessage());
    }

    // 2.5. Custom Business Exception: Wrong bulk-delete password
    @ExceptionHandler(InvalidDeletePasswordException.class)
    public ResponseEntity<Map<String, Object>> handleInvalidDeletePassword(InvalidDeletePasswordException ex) {
        log.warn("Bulk Delete Attempt — Wrong Password: {}", ex.getMessage());
        return buildResponse(HttpStatus.UNAUTHORIZED, "INVALID_DELETE_PASSWORD", ex.getMessage());
    }

    // 3. File Upload Exceptions (Max Size Exceeded or Bad Upload)
    @ExceptionHandler(MaxUploadSizeExceededException.class)
    public ResponseEntity<Map<String, Object>> handleMaxSizeException(MaxUploadSizeExceededException ex) {
        log.error("File upload size exceeded: {}", ex.getMessage());
        return buildResponse(HttpStatus.PAYLOAD_TOO_LARGE, "FILE_TOO_LARGE", "Uploaded CSV file exceeds the maximum allowed limit.");
    }

    @ExceptionHandler(MultipartException.class)
    public ResponseEntity<Map<String, Object>> handleMultipartException(MultipartException ex) {
        log.error("Multipart/File Upload Error: {}", ex.getMessage());
        return buildResponse(HttpStatus.BAD_REQUEST, "BAD_FILE_UPLOAD", "Error processing uploaded file. Please select a valid CSV file.");
    }

    // 4. Request Validation Errors (e.g., @Valid annotations failing)
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Map<String, Object>> handleValidationExceptions(MethodArgumentNotValidException ex) {
        log.error("Validation Error: {}", ex.getMessage());
        String detailMessage = ex.getBindingResult().getFieldErrors().stream()
                .map(err -> err.getField() + ": " + err.getDefaultMessage())
                .findFirst()
                .orElse("Validation failed");
        return buildResponse(HttpStatus.BAD_REQUEST, "VALIDATION_FAILED", detailMessage);
    }

    // 5. Missing Request Parameters (e.g., required input params missing in API)
    @ExceptionHandler(MissingServletRequestParameterException.class)
    public ResponseEntity<Map<String, Object>> handleMissingParams(MissingServletRequestParameterException ex) {
        log.error("Missing Request Parameter: {}", ex.getParameterName());
        return buildResponse(HttpStatus.BAD_REQUEST, "MISSING_PARAMETER", "Required parameter '" + ex.getParameterName() + "' is missing.");
    }

    // 6. Database / Data Access Exception (SQL / Hibernate / Repository Errors)
    @ExceptionHandler(DataAccessException.class)
    public ResponseEntity<Map<String, Object>> handleDatabaseException(DataAccessException ex) {
        log.error("Database Error encountered: ", ex);
        return buildResponse(HttpStatus.INTERNAL_SERVER_ERROR, "DATABASE_ERROR", "A database error occurred while processing your request.");
    }

    // 7. Input/Output Processing Errors
    @ExceptionHandler(IOException.class)
    public ResponseEntity<Map<String, Object>> handleIOException(IOException ex) {
        log.error("I/O File Reading Error: {}", ex.getMessage());
        return buildResponse(HttpStatus.INTERNAL_SERVER_ERROR, "IO_ERROR", "Error reading or writing file data.");
    }

    // 8. Null Pointer Exception
    @ExceptionHandler(NullPointerException.class)
    public ResponseEntity<Map<String, Object>> handleNullPointer(NullPointerException ex) {
        log.error("Null Pointer Exception triggered: ", ex);
        return buildResponse(HttpStatus.INTERNAL_SERVER_ERROR, "NULL_POINTER_ERROR", "A system null pointer error occurred.");
    }

    // 9. Invalid Input Arguments (e.g., bad method inputs)
    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<Map<String, Object>> handleIllegalArgument(IllegalArgumentException ex) {
        log.error("Illegal Argument Exception: {}", ex.getMessage());
        return buildResponse(HttpStatus.BAD_REQUEST, "INVALID_ARGUMENT", ex.getMessage());
    }

    // 10. API Route/Endpoint Not Found Exception
    @ExceptionHandler(NoHandlerFoundException.class)
    public ResponseEntity<Map<String, Object>> handleNotFound(NoHandlerFoundException ex) {
        log.warn("404 Endpoint Not Found: {}", ex.getRequestURL());
        return buildResponse(HttpStatus.NOT_FOUND, "ENDPOINT_NOT_FOUND", "Requested endpoint was not found: " + ex.getRequestURL());
    }

    // 10.5. Missing Static Resource (e.g. favicon.ico, or any other file the
    // browser/client asks for that simply isn't present under static/).
    // This is a routine, harmless 404 caused by a missing file — NOT a real
    // server error — so it is logged quietly at DEBUG level instead of being
    // treated as "Unexpected Internal Error" by the generic handler below.
    @ExceptionHandler(NoResourceFoundException.class)
    public ResponseEntity<Map<String, Object>> handleStaticResourceNotFound(NoResourceFoundException ex) {
        log.debug("Static resource not found: {}", ex.getResourcePath());
        return buildResponse(HttpStatus.NOT_FOUND, "STATIC_RESOURCE_NOT_FOUND", "Requested resource not found.");
    }

    // 11. Generic Catch-All Handler (For any uncaught exception)
    @ExceptionHandler(Exception.class)
    public ResponseEntity<Map<String, Object>> handleGenericException(Exception ex) {
        log.error("Unexpected Internal Error: ", ex);
        return buildResponse(HttpStatus.INTERNAL_SERVER_ERROR, "INTERNAL_SERVER_ERROR", "An unexpected system error occurred: " + ex.getMessage());
    }
}