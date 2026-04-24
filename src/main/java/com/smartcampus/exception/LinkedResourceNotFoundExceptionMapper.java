package com.smartcampus.exception;

import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.ext.ExceptionMapper;
import javax.ws.rs.ext.Provider;
import java.util.Map;

@Provider
public class LinkedResourceNotFoundExceptionMapper implements ExceptionMapper<LinkedResourceNotFoundException> {
    @Override
    public Response toResponse(LinkedResourceNotFoundException e) {
        return Response.status(422) // Unprocessable Entity
                .type(MediaType.APPLICATION_JSON)
                .entity(Map.of(
                        "status", 422,
                        "error", "Unprocessable Entity",
                        "message", e.getMessage()
                )).build();
    }
}
