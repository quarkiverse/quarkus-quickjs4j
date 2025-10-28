package com.example;

import io.roastedroot.quickjs4j.annotations.ScriptInterface;

/**
 * Script interface for processing data with complex types.
 * JavaScript implementations will receive TypeScript type definitions for context methods.
 */
@ScriptInterface(context = DataProcessorContext.class)
public interface DataProcessor {

    /**
     * Processes a person and returns a result.
     *
     * @param person the person to process
     * @return calculation result
     */
    CalculationResult processPerson(Person person);

    /**
     * Creates a person from basic information.
     *
     * @param name the person's name
     * @param age the person's age
     * @param street the street address
     * @param city the city
     * @param zipCode the zip code
     * @return a new Person object
     */
    Person createPerson(String name, int age, String street, String city, String zipCode);
}
