package com.unireview.exception;

public class CaptchaFailedException extends RuntimeException {
    public CaptchaFailedException(String message) {
        super(message);
    }
}
