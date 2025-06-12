package com.bankingsystem.exception;

import com.fasterxml.jackson.databind.exc.InvalidFormatException;
import org.springframework.dao.DataAccessException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.context.request.WebRequest;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

@RestControllerAdvice
public class GlobalExceptionHandler {

    private ResponseEntity<Object> buildResponse(HttpStatus status, String message, WebRequest request) {
        Map<String, Object> error = new HashMap<>();
        error.put("timestamp", LocalDateTime.now());
        error.put("status", status.value());
        error.put("error", status.getReasonPhrase());
        error.put("message", message);
        error.put("path", request.getDescription(false).replace("uri=", ""));
        return new ResponseEntity<>(error, status);
    }

    @ExceptionHandler(UserNotFoundException.class)
    public ResponseEntity<Object> handleUserNotFound(UserNotFoundException ex, WebRequest request) {
        return buildResponse(HttpStatus.NOT_FOUND, ex.getMessage(), request);
    }

    @ExceptionHandler(AccountNotFoundException.class)
    public ResponseEntity<Object> handleAccountNotFound(AccountNotFoundException ex, WebRequest request) {
        return buildResponse(HttpStatus.NOT_FOUND, ex.getMessage(), request);
    }

    @ExceptionHandler(DataAccessException.class)
    public ResponseEntity<Object> handleDataAccessException(DataAccessException ex, WebRequest request) {
        return buildResponse(HttpStatus.INTERNAL_SERVER_ERROR, "Database error occurred. Please try again later.", request);
    }

    @ExceptionHandler(InsufficientBalanceException.class)
    public ResponseEntity<Object> handleInsufficientBalance(InsufficientBalanceException ex, WebRequest request) {
        return buildResponse(HttpStatus.BAD_REQUEST, ex.getMessage(), request);
    }

    @ExceptionHandler(InvalidTransferException.class)
    public ResponseEntity<Object> handleInvalidTransfer(InvalidTransferException ex, WebRequest request) {
        return buildResponse(HttpStatus.BAD_REQUEST, ex.getMessage(), request);
    }

    @ExceptionHandler(UserAlreadyExistsException.class)
    public ResponseEntity<Object> handleUserAlreadyExists(UserAlreadyExistsException ex, WebRequest request) {
        return buildResponse(HttpStatus.BAD_REQUEST, ex.getMessage(), request);
    }

    @ExceptionHandler(InvalidCredentialsException.class)
    public ResponseEntity<Object> handleInvalidCredentials(InvalidCredentialsException ex, WebRequest request) {
        return buildResponse(HttpStatus.UNAUTHORIZED, ex.getMessage(), request);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<Object> handleGeneralException(Exception ex, WebRequest request) {
        ex.printStackTrace(); // Optional: log it in production
        return buildResponse(HttpStatus.INTERNAL_SERVER_ERROR, "An unexpected error occurred.", request);
    }

    @ExceptionHandler(InvalidUserRoleException.class)
    public ResponseEntity<Object> handleInvalidUserRole(InvalidUserRoleException ex, WebRequest request) {
        return buildResponse(HttpStatus.BAD_REQUEST, ex.getMessage(), request);
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Object> handleValidationException(MethodArgumentNotValidException ex, WebRequest request) {
        String msg = ex.getBindingResult().getFieldErrors().stream()
                .map(e -> e.getField() + ": " + e.getDefaultMessage())
                .reduce((m1, m2) -> m1 + "; " + m2)
                .orElse("Validation failed");
        return buildResponse(HttpStatus.BAD_REQUEST, msg, request);
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<Object> handleHttpMessageNotReadable(HttpMessageNotReadableException ex, WebRequest request) {
        Throwable cause = ex.getCause();
        if (cause instanceof InvalidFormatException) {
            InvalidFormatException ife = (InvalidFormatException) cause;
            String fieldName = ife.getPath().isEmpty() ? "unknown" : ife.getPath().get(0).getFieldName();
            String invalidValue = ife.getValue() != null ? ife.getValue().toString() : "null";
            String msg;

            if ("role".equals(fieldName)) {
                msg = String.format("Invalid value '%s' for field '%s'. Allowed roles are: CUSTOMER, ADMIN.",
                        invalidValue, fieldName);
            } else if ("amount".equals(fieldName)) {
                msg = String.format(
                        "Invalid value '%s' for field '%s'. Please provide a valid numeric amount (e.g., 1000.00).",
                        invalidValue, fieldName);
            } else {
                msg = String.format("Invalid value '%s' for field '%s'.", invalidValue, fieldName);
            }
            return buildResponse(HttpStatus.BAD_REQUEST, msg, request);
        }
        return buildResponse(HttpStatus.BAD_REQUEST, "Malformed JSON request.", request);
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<Object> handleIllegalArgumentException(IllegalArgumentException ex, WebRequest request) {
        return buildResponse(HttpStatus.BAD_REQUEST, ex.getMessage(), request);
    }

    @ExceptionHandler(org.springframework.security.access.AccessDeniedException.class)
    public ResponseEntity<Object> handleSpringAccessDeniedException(
            org.springframework.security.access.AccessDeniedException ex, WebRequest request) {
        String msg = ex.getMessage() != null ? ex.getMessage() : "You do not have permission to perform this action.";
        return buildResponse(HttpStatus.FORBIDDEN, msg, request);
    }

    // Handles business logic access denied (e.g., an account does not belong to user)
    @ExceptionHandler(java.nio.file.AccessDeniedException.class)
    public ResponseEntity<Object> handleNioAccessDeniedException(java.nio.file.AccessDeniedException ex,
            WebRequest request) {
        return buildResponse(HttpStatus.FORBIDDEN,
                ex.getMessage() != null ? ex.getMessage() : "You do not have permission to access this resource.",
                request);
    }

    @ExceptionHandler(com.fasterxml.jackson.databind.exc.InvalidFormatException.class)
    public ResponseEntity<Object> handleInvalidFormatException(
            com.fasterxml.jackson.databind.exc.InvalidFormatException ex, WebRequest request) {
        String fieldName = ex.getPath().isEmpty() ? "unknown" : ex.getPath().get(0).getFieldName();
        String invalidValue = ex.getValue() != null ? ex.getValue().toString() : "null";
        String msg;

        if ("role".equals(fieldName)) {
            msg = String.format("Invalid value '%s' for field '%s'. Allowed roles are: CUSTOMER, ADMIN.", invalidValue,
                    fieldName);
        } else if ("amount".equals(fieldName)) {
            msg = String.format(
                    "Invalid value '%s' for field '%s'. Please provide a valid numeric amount (e.g., 1000.00).",
                    invalidValue, fieldName);
        } else {
            msg = String.format("Invalid value '%s' for field '%s'.", invalidValue, fieldName);
        }
        return buildResponse(HttpStatus.BAD_REQUEST, msg, request);
    }

    @ExceptionHandler(InactiveAccountException.class)
    public ResponseEntity<Object> handleInactiveAccountException(InactiveAccountException ex, WebRequest request) {
        return buildResponse(HttpStatus.BAD_REQUEST, ex.getMessage(), request);
    }

    @ExceptionHandler(ResetTokenInvalidException.class)
    public ResponseEntity<Object> handleResetTokenInvalid(ResetTokenInvalidException ex, WebRequest request) {
        return buildResponse(HttpStatus.BAD_REQUEST, ex.getMessage(), request);
    }

    @ExceptionHandler(ResetTokenExpiredException.class)
    public ResponseEntity<Object> handleResetTokenExpired(ResetTokenExpiredException ex, WebRequest request) {
        return buildResponse(HttpStatus.BAD_REQUEST, ex.getMessage(), request);
    }

    @ExceptionHandler(EmailSendFailedException.class)
    public ResponseEntity<Object> handleEmailSendFailure(EmailSendFailedException ex, WebRequest request) {
        return buildResponse(HttpStatus.INTERNAL_SERVER_ERROR, "Failed to send email: " + ex.getMessage(), request);
    }

    @ExceptionHandler(PasswordResetException.class)
    public ResponseEntity<Object> handlePasswordReset(PasswordResetException ex, WebRequest request) {
        return buildResponse(HttpStatus.BAD_REQUEST, ex.getMessage(), request);
    }


}
