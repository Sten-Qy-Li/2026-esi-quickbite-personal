package ee.ut.esi.quickbite.restaurant.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import ee.ut.esi.quickbite.restaurant.exception.ErrorResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.time.OffsetDateTime;

@Component
public class RestAuthEntryPoints {

    private final ObjectMapper objectMapper;

    public RestAuthEntryPoints(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    public AuthenticationEntryPoint unauthorizedEntryPoint() {
        return (HttpServletRequest request, HttpServletResponse response, AuthenticationException ex) ->
            writeError(request, response, HttpStatus.UNAUTHORIZED, "Authentication required");
    }

    public AccessDeniedHandler forbiddenHandler() {
        return (HttpServletRequest request, HttpServletResponse response, AccessDeniedException ex) ->
            writeError(request, response, HttpStatus.FORBIDDEN, "Access denied");
    }

    private void writeError(HttpServletRequest request, HttpServletResponse response,
                            HttpStatus status, String message) throws IOException {
        response.setStatus(status.value());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        ErrorResponse body = new ErrorResponse(
            OffsetDateTime.now(),
            status.value(),
            status.getReasonPhrase(),
            message,
            request.getRequestURI(),
            null
        );
        objectMapper.writeValue(response.getWriter(), body);
    }
}
