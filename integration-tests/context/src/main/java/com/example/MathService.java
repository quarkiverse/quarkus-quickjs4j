package com.example;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import io.quarkus.arc.Unremovable;

@Unremovable
@ApplicationScoped
public class MathService {

    @Inject
    Calculator calculator;

    public int performCalculation() {
        int sum = calculator.add(5, 3); // Returns 8
        int product = calculator.multiply(4, 7); // Returns 28
        double quotient = calculator.divide(10.0, 2.0); // Returns 5.0

        return sum + product + (int) quotient;
    }
}
