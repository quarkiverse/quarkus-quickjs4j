package com.example;

import io.quarkiverse.quickjs4j.annotations.ScriptImplementation;
import io.roastedroot.quickjs4j.annotations.ScriptInterface;

@ScriptInterface(context = CalculatorContext.class)
@ScriptImplementation(location = "dynamicCalculatorWithContext.js")
public interface Calculator {
    int add(int a, int b);

    int multiply(int a, int b);

    double divide(double a, double b);
}
