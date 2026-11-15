package com.example.alumni.exception;

import java.util.stream.Collectors;

import com.example.alumni.dto.ApiResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.ErrorResponse;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
@Slf4j
public class GlobalExceptionHandler {

	@ExceptionHandler(MethodArgumentNotValidException.class)
	public ResponseEntity<ApiResponse<Void>> invalid(MethodArgumentNotValidException e) {
		String message = e.getBindingResult().getFieldErrors().stream()
				.map(error -> error.getDefaultMessage()).sorted().collect(Collectors.joining("; "));
		return ResponseEntity.badRequest().body(ApiResponse.error(message));
	}

	@ExceptionHandler(HttpMessageNotReadableException.class)
	public ResponseEntity<ApiResponse<Void>> unreadable(HttpMessageNotReadableException e) {
		return ResponseEntity.badRequest().body(ApiResponse.error(
				"Malformed request body: expected JSON with university, designation and optional numeric passoutYear"));
	}

	@ExceptionHandler(PhantomBusterException.class)
	public ResponseEntity<ApiResponse<Void>> phantomBuster(PhantomBusterException e) {
		log.error("PhantomBuster error", e);
		return ResponseEntity.status(HttpStatus.BAD_GATEWAY).body(ApiResponse.error(e.getMessage()));
	}

	@ExceptionHandler(Exception.class)
	public ResponseEntity<ApiResponse<Void>> unexpected(Exception e) {
		if (e instanceof ErrorResponse response) { // Spring's own 4xx errors (404, 405, 415, ...)
			return ResponseEntity.status(response.getStatusCode()).body(ApiResponse.error(e.getMessage()));
		}
		log.error("Unexpected error", e);
		return ResponseEntity.internalServerError().body(ApiResponse.error("Internal server error"));
	}
}
