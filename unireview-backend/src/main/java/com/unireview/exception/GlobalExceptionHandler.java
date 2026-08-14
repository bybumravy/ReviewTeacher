package com.unireview.exception;

import com.unireview.dto.response.ErrorResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.time.LocalDateTime;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleNotFound(ResourceNotFoundException ex) {
        return buildResponse(HttpStatus.NOT_FOUND, "NOT_FOUND", ex.getMessage());
    }

    @ExceptionHandler(InsufficientCreditException.class)
    public ResponseEntity<ErrorResponse> handleInsufficientCredit(InsufficientCreditException ex) {
        return buildResponse(HttpStatus.FORBIDDEN, "INSUFFICIENT_CREDIT", ex.getMessage());
    }

    @ExceptionHandler(NoReviewerTokenException.class)
    public ResponseEntity<ErrorResponse> handleNoToken(NoReviewerTokenException ex) {
        return buildResponse(HttpStatus.FORBIDDEN, "NO_REVIEWER_TOKEN", ex.getMessage());
    }

    @ExceptionHandler(DuplicateReviewException.class)
    public ResponseEntity<ErrorResponse> handleDuplicateReview(DuplicateReviewException ex) {
        return buildResponse(HttpStatus.BAD_REQUEST, "DUPLICATE_REVIEW", ex.getMessage());
    }

    @ExceptionHandler(ContentViolationException.class)
    public ResponseEntity<ErrorResponse> handleContentViolation(ContentViolationException ex) {
        return buildResponse(HttpStatus.BAD_REQUEST, "CONTENT_VIOLATION", ex.getMessage());
    }

    @ExceptionHandler(CaptchaFailedException.class)
    public ResponseEntity<ErrorResponse> handleCaptchaFailed(CaptchaFailedException ex) {
        return buildResponse(HttpStatus.BAD_REQUEST, "CAPTCHA_FAILED", ex.getMessage());
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleValidation(MethodArgumentNotValidException ex) {
        FieldError fieldError = ex.getBindingResult().getFieldError();
        String msg = fieldError != null ? fieldError.getField() + ": " + fieldError.getDefaultMessage() : "Validation error";
        return buildResponse(HttpStatus.BAD_REQUEST, "VALIDATION_ERROR", msg);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleGeneral(Exception ex) {
        return buildResponse(HttpStatus.INTERNAL_SERVER_ERROR, "SERVER_ERROR", ex.getMessage());
    }

    private ResponseEntity<ErrorResponse> buildResponse(HttpStatus status, String error, String message) {
        ErrorResponse err = ErrorResponse.builder()
                .status(status.value())
                .error(error)
                .message(message)
                .timestamp(LocalDateTime.now())
                .build();
        return new ResponseEntity<>(err, status);
    }
}
