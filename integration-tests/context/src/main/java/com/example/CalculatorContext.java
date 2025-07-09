package com.example;

import jakarta.enterprise.context.ApplicationScoped;

@ApplicationScoped
public class CalculatorContext {

    public int javaMultiply(int a, int b) {
        return a * b;
    }

}
