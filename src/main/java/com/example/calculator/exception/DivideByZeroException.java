package com.example.calculator.exception;

public class DivideByZeroException extends RuntimeException {

    public DivideByZeroException(String message) {
        super(message);
    }
}
