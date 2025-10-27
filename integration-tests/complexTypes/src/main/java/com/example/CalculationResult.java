package com.example;

import java.time.LocalDateTime;
import java.util.List;

/**
 * CalculationResult bean for testing complex type mapping.
 */
public class CalculationResult {

    private double value;
    private String message;
    private LocalDateTime timestamp;
    private List<String> tags;

    public CalculationResult() {
    }

    public CalculationResult(double value, String message, LocalDateTime timestamp, List<String> tags) {
        this.value = value;
        this.message = message;
        this.timestamp = timestamp;
        this.tags = tags;
    }

    public double getValue() {
        return value;
    }

    public void setValue(double value) {
        this.value = value;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public LocalDateTime getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(LocalDateTime timestamp) {
        this.timestamp = timestamp;
    }

    public List<String> getTags() {
        return tags;
    }

    public void setTags(List<String> tags) {
        this.tags = tags;
    }
}