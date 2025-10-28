package com.example;

import jakarta.enterprise.context.ApplicationScoped;

/**
 * Context class providing Java methods accessible from JavaScript.
 * Tests complex type mapping with Java beans.
 */
@ApplicationScoped
public class DataProcessorContext {

    /**
     * Validates a person object.
     *
     * @param person the person to validate
     * @return true if person is valid
     */
    public boolean validatePerson(Person person) {
        return person != null &&
                person.getName() != null &&
                !person.getName().isEmpty() &&
                person.getAge() > 0;
    }

    /**
     * Creates an address from components.
     *
     * @param street the street
     * @param city the city
     * @param zipCode the zip code
     * @return a new Address object
     */
    public Address createAddress(String street, String city, String zipCode) {
        return new Address(street, city, zipCode);
    }

    /**
     * Formats a person's full address.
     *
     * @param person the person
     * @return formatted address string
     */
    public String formatAddress(Person person) {
        if (person == null || person.getAddress() == null) {
            return "No address";
        }
        Address addr = person.getAddress();
        return addr.getStreet() + ", " + addr.getCity() + " " + addr.getZipCode();
    }
}
