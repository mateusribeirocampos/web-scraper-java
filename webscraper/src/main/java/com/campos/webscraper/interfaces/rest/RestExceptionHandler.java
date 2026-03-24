package com.campos.webscraper.interfaces.rest;

import com.campos.webscraper.interfaces.dto.ErrorResponse;
import com.campos.webscraper.interfaces.dto.TargetSiteActivationBlockedErrorResponse;
import com.campos.webscraper.application.onboarding.TargetSiteOnboardingProfileNotFoundException;
import com.campos.webscraper.shared.CrawlJobNotFoundException;
import com.campos.webscraper.shared.TargetSiteActivationBlockedException;
import com.campos.webscraper.shared.TargetSiteNotFoundException;
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

    @ExceptionHandler(TargetSiteNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleTargetSiteNotFound(TargetSiteNotFoundException exception) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(new ErrorResponse(exception.getMessage()));
    }

    @ExceptionHandler(TargetSiteOnboardingProfileNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleTargetSiteOnboardingProfileNotFound(
            TargetSiteOnboardingProfileNotFoundException exception
    ) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(new ErrorResponse(exception.getMessage()));
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ErrorResponse> handleIllegalArgument(IllegalArgumentException exception) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(new ErrorResponse(exception.getMessage()));
    }

    @ExceptionHandler(TargetSiteActivationBlockedException.class)
    public ResponseEntity<TargetSiteActivationBlockedErrorResponse> handleTargetSiteActivationBlocked(
            TargetSiteActivationBlockedException exception
    ) {
        return ResponseEntity.status(HttpStatus.CONFLICT)
                .body(new TargetSiteActivationBlockedErrorResponse(
                        exception.getMessage(),
                        exception.getBlockingReasons()
                ));
    }
}
