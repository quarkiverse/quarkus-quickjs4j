/**
 * JavaScript implementation of DataProcessor interface.
 * Uses context methods from DataProcessorContext for complex operations.
 */

function createPerson(name, age, street, city, zipCode) {
    // Use context method to create address
    const address = DataProcessor_Builtins.createAddress(street, city, zipCode);

    return {
        name: name,
        age: age,
        address: address
    };
}

function processPerson(person) {
    // Validate the person using context method
    if (!DataProcessor_Builtins.validatePerson(person)) {
        return {
            value: 0.0,
            message: "Invalid person data",
            timestamp: null,
            tags: ["error", "validation"]
        };
    }

    // Format address using context method
    const formattedAddress = DataProcessor_Builtins.formatAddress(person);

    return {
        value: person.age * 1.5,
        message: "Processed: " + person.name + " at " + formattedAddress,
        timestamp: null,
        tags: ["success", "processed"]
    };
}

export {
    processPerson,
    createPerson
};