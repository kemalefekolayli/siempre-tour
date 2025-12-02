package Exceptions;

import java.time.Instant;
import java.util.List;
import java.util.stream.Collectors;

import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleMethodArgumentNotValidException(MethodArgumentNotValidException ex) {
        List<FieldError> fieldErrors = ex.getBindingResult().getFieldErrors();

        String errors = fieldErrors.stream()
                .map(error -> error.getField() + ": " + error.getDefaultMessage() + " [ rejected value: " + error.getRejectedValue() + " ]")
                .collect(Collectors.joining(", "));

        ErrorResponse response = new ErrorResponse(
                Instant.now(),
                ErrorCodes.VALIDATION_ERROR.getStatus().value(),
                ErrorCodes.VALIDATION_ERROR.getCode(),
                errors,
                ErrorCodes.VALIDATION_ERROR.getMessage()
        );

        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
    }

    @ExceptionHandler(GlobalException.class)
    public ResponseEntity<ErrorResponse> handleCustomException(GlobalException ex) {
        ErrorCodes code = ex.getErrorCode();
        ErrorResponse body = new ErrorResponse(Instant.now(), code.getStatus().value(), code.getCode(), ex.getMessage(), code.getStatus().getReasonPhrase());

        return new ResponseEntity<ErrorResponse>(body, code.getStatus());
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleGenericException(Exception ex) {
        log.error(ex.getMessage());
        ErrorCodes code = ErrorCodes.INTERNAL_SERVER_ERROR;
        ErrorResponse body = new ErrorResponse(Instant.now(), code.getStatus().value(), code.getCode(), ex.getMessage(), code.getStatus().getReasonPhrase());

        return new ResponseEntity<ErrorResponse>(body, code.getStatus());
    }
}