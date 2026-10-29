package com.example.calculator.service;

import com.example.calculator.exception.DivideByZeroException;

public class CalculatorService {

    public double add(double a, double b) {
        validate(a, b);
        return a + b;
    }

    public double subtract(double a, double b) {
        validate(a, b);
        return a - b;
    }

    public double multiply(double a, double b) {
        validate(a, b);
        return a * b;
    }

    public double divide(double a, double b) {
        validate(a, b);
        if (b == 0) {
            throw new DivideByZeroException("Division by zero is not allowed");
        }
        return a / b;
    }

    /** Rejects NaN and infinite inputs, which would otherwise leak into results. */
    private void validate(double a, double b) {
        if (!Double.isFinite(a) || !Double.isFinite(b)) {
            throw new IllegalArgumentException("Operands must be finite numbers");
        }
    }
}
