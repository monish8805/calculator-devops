package com.example.calculator.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.example.calculator.exception.DivideByZeroException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class CalculatorServiceTest {

    private CalculatorService service;

    @BeforeEach
    void setUp() {
        service = new CalculatorService();
    }

    @Test
    void addReturnsSum() {
        assertEquals(5.0, service.add(2, 3));
    }

    @Test
    void addHandlesNegatives() {
        assertEquals(-1.0, service.add(-4, 3));
    }

    @Test
    void subtractReturnsDifference() {
        assertEquals(7.0, service.subtract(10, 3));
    }

    @Test
    void multiplyReturnsProduct() {
        assertEquals(12.0, service.multiply(3, 4));
    }

    @Test
    void multiplyByZeroReturnsZero() {
        assertEquals(0.0, service.multiply(99, 0));
    }

    @Test
    void divideReturnsQuotient() {
        assertEquals(2.5, service.divide(5, 2));
    }

    @Test
    void divideByZeroThrows() {
        assertThrows(DivideByZeroException.class, () -> service.divide(5, 0));
    }

    @Test
    void nonFiniteOperandIsRejected() {
        assertThrows(IllegalArgumentException.class, () -> service.add(Double.NaN, 1));
        assertThrows(IllegalArgumentException.class, () -> service.multiply(Double.POSITIVE_INFINITY, 2));
    }
}
