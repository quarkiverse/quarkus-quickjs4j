package com.example;

import io.roastedroot.quickjs4j.annotations.ScriptInterface;
import io.quarkiverse.quickjs4j.annotations.ScriptImplementation;

@ScriptInterface
@ScriptImplementation(location = "calculator.js")
public interface Calculator {
    int add(int a, int b);
    int multiply(int a, int b);
    double divide(double a, double b);
}
