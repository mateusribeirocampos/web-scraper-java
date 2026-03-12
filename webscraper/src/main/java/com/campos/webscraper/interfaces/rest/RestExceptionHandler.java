package com.campos.webscraper.interfaces.rest;

import com.campos.webscraper.interfaces.dto.ErrorResponse;
import com.campos.webscraper.shared.CrawlJobNotFoundException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/**
 * Maps application exceptions to REST responses.
 */
@RestControllerAdvice
public class RestExceptionHandler {

    @ExceptionHandler(CrawlJobNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleCrawlJobNotFound(CrawlJobNotFoundException exception) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(new ErrorResponse(exception.getMessage()));
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ErrorResponse> handleIllegalArgument(IllegalArgumentException exception) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(new ErrorResponse(exception.getMessage()));
    }
}
