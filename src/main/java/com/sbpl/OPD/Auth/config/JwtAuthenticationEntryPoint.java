package com.sbpl.OPD.Auth.config;

import com.sbpl.OPD.response.BaseResponse;
import com.sbpl.OPD.response.ResponseDto;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.stereotype.Component;
import tools.jackson.databind.ObjectMapper;

import java.io.IOException;

@Component
public class JwtAuthenticationEntryPoint implements AuthenticationEntryPoint {

    @Autowired
    private BaseResponse baseResponse;

    @Override
    public void commence(HttpServletRequest request, HttpServletResponse response,
                         AuthenticationException authException) throws IOException {
        
        // Create consistent error response using BaseResponse
        ResponseDto errorResponse = new ResponseDto();
        errorResponse.setResponse(false);
        errorResponse.setMessage("Authentication required: " + determineErrorMessage(authException));
        errorResponse.setStatus(HttpStatus.UNAUTHORIZED);
        errorResponse.setTimestamp(java.time.LocalDateTime.now());
        
        // Set response content type and status
        response.setContentType("application/json");
        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        
        // Write the JSON response
        ObjectMapper objectMapper = new ObjectMapper();
        String jsonResponse = objectMapper.writeValueAsString(errorResponse);
        response.getWriter().write(jsonResponse);
    }
    
    private String determineErrorMessage(AuthenticationException authException) {
        if (authException != null) {
            String message = authException.getMessage();
            if (message != null && !message.isEmpty()) {
                return message;
            }
        }
        
        return "You need to be authenticated to access this resource. Please log in again.";
    }
}