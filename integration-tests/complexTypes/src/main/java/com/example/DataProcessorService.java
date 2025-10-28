package com.example;

import jakarta.inject.Inject;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;

import io.quarkiverse.quickjs4j.ScriptInterfaceFactory;
import io.quarkiverse.quickjs4j.util.ScriptInterfaceUtils;

/**
 * REST resource for testing DataProcessor with complex types.
 */
@Path("/data-processor")
public class DataProcessorService {

    @Inject
    ScriptInterfaceFactory<DataProcessor, DataProcessorContext> dataProcessorFactory;

    @Inject
    DataProcessorContext context;

    /**
     * Test endpoint that creates a person and processes it.
     *
     * @return the calculation result
     */
    @GET
    @Path("/process")
    @Produces(MediaType.APPLICATION_JSON)
    public CalculationResult processTestPerson() {
        String scriptLibrary = ScriptInterfaceUtils.loadScriptLibrary("dataProcessor.js");
        DataProcessor processor = dataProcessorFactory.create(scriptLibrary, context);

        Person person = processor.createPerson("Alice", 30, "123 Main St", "Springfield", "12345");
        return processor.processPerson(person);
    }
}
