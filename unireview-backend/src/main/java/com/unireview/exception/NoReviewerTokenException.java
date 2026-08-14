package com.unireview.exception;

public class NoReviewerTokenException extends RuntimeException {
    public NoReviewerTokenException(String message) {
        super(message);
    }
}
