package com.example;

import io.quarkiverse.quickjs4j.annotations.ScriptImplementation;
import io.roastedroot.quickjs4j.annotations.ScriptInterface;

@ScriptInterface
@ScriptImplementation(location = "calculator.js")
public interface Calculator {
    int add(int a, int b);

    int multiply(int a, int b);

    double divide(double a, double b);
}
