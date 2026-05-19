package com.lianzhi.wms.exception;

import jakarta.validation.ConstraintViolationException;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.stream.Collectors;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.server.ResponseStatusException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@RestControllerAdvice
public class GlobalExceptionHandler {
  private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

  @ExceptionHandler(MethodArgumentNotValidException.class)
  public ResponseEntity<ErrorResponse> handleValidation(MethodArgumentNotValidException ex) {
    Map<String, String> errors = ex.getBindingResult().getFieldErrors().stream()
        .collect(Collectors.toMap(
            FieldError::getField,
            FieldError::getDefaultMessage,
            (oldValue, newValue) -> oldValue,
            LinkedHashMap::new
        ));
    return ResponseEntity.badRequest().body(new ErrorResponse("参数校验失败", errors));
  }

  @ExceptionHandler(ConstraintViolationException.class)
  public ResponseEntity<ErrorResponse> handleConstraint(ConstraintViolationException ex) {
    return ResponseEntity.badRequest().body(new ErrorResponse("参数校验失败: " + ex.getMessage(), null));
  }

  @ExceptionHandler(HttpMessageNotReadableException.class)
  public ResponseEntity<ErrorResponse> handleMessageNotReadable(HttpMessageNotReadableException ex) {
    log.warn("Request body parse error", ex);
    return ResponseEntity.badRequest().body(new ErrorResponse("请求体格式错误或字段类型不正确", null));
  }

  @ExceptionHandler(ResponseStatusException.class)
  public ResponseEntity<ErrorResponse> handleResponseStatus(ResponseStatusException ex) {
    HttpStatus status = HttpStatus.valueOf(ex.getStatusCode().value());
    String message = ex.getReason() == null ? "请求失败" : ex.getReason();
    return ResponseEntity.status(status).body(new ErrorResponse(message, null));
  }

  @ExceptionHandler({EmptyResultDataAccessException.class, NoSuchElementException.class})
  public ResponseEntity<ErrorResponse> handleNotFound(Exception ex) {
    return ResponseEntity.status(HttpStatus.NOT_FOUND).body(new ErrorResponse("数据不存在", null));
  }

  @ExceptionHandler(DataIntegrityViolationException.class)
  public ResponseEntity<ErrorResponse> handleIntegrity(DataIntegrityViolationException ex) {
    log.warn("Data integrity violation", ex);
    return ResponseEntity.status(HttpStatus.CONFLICT)
        .body(new ErrorResponse("当前数据被其他业务引用，无法执行该操作", null));
  }

  @ExceptionHandler(Exception.class)
  public ResponseEntity<ErrorResponse> handleUnknown(Exception ex) {
    log.error("Unhandled server error", ex);
    return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
        .body(new ErrorResponse("服务器内部错误", null));
  }

  public record ErrorResponse(String message, Map<String, String> errors) {}
}
