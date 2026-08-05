package sm.selflearn.samskrtam.curriculum.exception;

import jakarta.persistence.EntityNotFoundException;
import org.springframework.http.HttpStatus;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import sm.selflearn.samskrtam.curriculum.dto.ErrorResponseDto;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(EntityNotFoundException.class)
    @ResponseStatus(HttpStatus.NOT_FOUND)
    public ErrorResponseDto handleNotFound(EntityNotFoundException e) {
        return new ErrorResponseDto(HttpStatus.NOT_FOUND.value(), "Not found", e.getMessage());
    }

    @ExceptionHandler({TopicCycleException.class, DuplicateCodeException.class})
    @ResponseStatus(HttpStatus.CONFLICT)
    public ErrorResponseDto handleConflict(RuntimeException e) {
        return new ErrorResponseDto(HttpStatus.CONFLICT.value(), "Conflict", e.getMessage());
    }

    @ExceptionHandler(InvalidComplexQuizCompositionException.class)
    @ResponseStatus(HttpStatus.UNPROCESSABLE_ENTITY)
    public ErrorResponseDto handleInvalidComposition(InvalidComplexQuizCompositionException e) {
        return new ErrorResponseDto(HttpStatus.UNPROCESSABLE_ENTITY.value(), "Unprocessable Entity", e.getMessage());
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ErrorResponseDto handleValidation(MethodArgumentNotValidException e) {
        String details = e.getBindingResult().getFieldErrors().stream()
                .map(error -> error.getField() + ": " + error.getDefaultMessage())
                .reduce((a, b) -> a + "; " + b)
                .orElse(e.getMessage());
        return new ErrorResponseDto(HttpStatus.BAD_REQUEST.value(), "Validation failed", details);
    }

    @ExceptionHandler({HttpMessageNotReadableException.class, MethodArgumentTypeMismatchException.class})
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ErrorResponseDto handleBadRequest(Exception e) {
        return new ErrorResponseDto(HttpStatus.BAD_REQUEST.value(), "Bad request", e.getMessage());
    }
}
