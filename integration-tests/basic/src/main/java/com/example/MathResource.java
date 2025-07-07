package com.example;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;

@Path("/math")
@ApplicationScoped
public class MathResource {
    @Inject
    MathService math;

    @GET
    @Produces(MediaType.APPLICATION_JSON)
    public int run() {
        return math.performCalculation();
    }
}
