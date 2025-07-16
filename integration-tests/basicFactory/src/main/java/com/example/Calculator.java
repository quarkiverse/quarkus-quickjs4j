package com.example;

import io.roastedroot.quickjs4j.annotations.ScriptInterface;

@ScriptInterface
public interface Calculator {
    int add(int a, int b);

    int multiply(int a, int b);

    double divide(double a, double b);
}
