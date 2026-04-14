package com.sbpl.OPD.serviceImp;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sbpl.OPD.response.BaseResponse;
import com.sbpl.OPD.service.Msg91SmsService;
import org.apache.hc.client5.http.classic.methods.HttpPost;
import org.apache.hc.client5.http.impl.classic.CloseableHttpClient;
import org.apache.hc.client5.http.impl.classic.CloseableHttpResponse;
import org.apache.hc.client5.http.impl.classic.HttpClients;
import org.apache.hc.core5.http.io.entity.StringEntity;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class Msg91SmsServiceImpl implements Msg91SmsService {

    private static final Logger logger = LoggerFactory.getLogger(Msg91SmsServiceImpl.class);
    
    @Autowired
    private BaseResponse baseResponse;
    
    private final ObjectMapper objectMapper = new ObjectMapper();
    
    @Value("${msg91.auth.key}")
    private String authKey;
    
    @Value("${msg91.template.id}")
    private String templateId;

    @Value("${reschedule-sms-templateId}")
    private String rescheduleSmsTemplateId;

    @Value("${create-sms-templateId}")
    private String createSmsTemplateId;

    @Value("${cancel-sms-templateId}")
    private String cancelSmsTemplateId;

    @Value("${complete-sms-templateId}")
    private String completeSmsTemplateId;
    
    @Value("${msg91.sender.id}")
    private String senderId;
    
    @Value("${msg91.route}")
    private String route;
    
    @Value("${msg91.country}")
    private String country;
    
    @Value("${msg91.enabled}")
    private boolean smsEnabled;
    
    @Value("${msg91.api.url}")
    private String apiUrl;
    
    @Value("${msg91.retry.attempts}")
    private int maxRetryAttempts;
    
    @Value("${msg91.retry.delay.ms}")
    private long retryDelayMs;

    @Override
    public ResponseEntity<?> sendSms(String phoneNumber, String message) {
        if (!smsEnabled) {
            logger.info("SMS service is disabled. Skipping SMS to {}", phoneNumber);
            return baseResponse.successResponse("SMS service is disabled");
        }

        if (!isValidPhoneNumber(phoneNumber)) {
            logger.warn("Invalid phone number format: {}", phoneNumber);
            return baseResponse.errorResponse(HttpStatus.BAD_REQUEST, "Invalid phone number format");
        }

        // Format phone number (remove spaces, dashes, etc.)
        String formattedPhone = formatPhoneNumber(phoneNumber);

        Map<String, Object> data = new HashMap<>();
        data.put("mobiles", country + formattedPhone);
        data.put("VAR1","");
        data.put("VAR2", "");
        
        // Prepare request payload
        Map<String, Object> payload = new HashMap<>(6, 1);
        payload.put("template_id", templateId);
        payload.put("short_url", "");
        payload.put("short_url_expiry", ")");
        payload.put("realTimeResponse", "");
        payload.put("recipients", List.of(data));

        return sendSmsWithRetry(payload, 0);
    }

    @Override
    public ResponseEntity<?> sendAppointmentReminderSms(
            String patientName,
            String phoneNumber,
            String doctorName,
            String appointmentDateTime,
            String appointmentId,
            String companyName) {
        
        if (!smsEnabled) {
            logger.info("SMS service is disabled. Skipping appointment reminder SMS to {}", patientName);
            return baseResponse.successResponse("SMS service is disabled");
        }

        if (!isValidPhoneNumber(phoneNumber)) {
            logger.warn("Invalid phone number for patient {}: {}", patientName, phoneNumber);
            return baseResponse.errorResponse(HttpStatus.BAD_REQUEST, "Invalid phone number format");
        }

        String formattedPhone = formatPhoneNumber(phoneNumber);

        Map<String, Object> data = new HashMap<>();
        data.put("mobiles", country + formattedPhone);
        data.put("var1",appointmentDateTime);
        data.put("var2", companyName);
        data.put("var3", appointmentId);

        // Prepare request payload
        Map<String, Object> payload = new HashMap<>(6, 1);
        payload.put("template_id", rescheduleSmsTemplateId);
        payload.put("short_url", "");
        payload.put("short_url_expiry", ")");
        payload.put("realTimeResponse", "");
        payload.put("recipients", List.of(data));

        return sendSmsWithRetry(payload, 0);
    }

    @Override
    public ResponseEntity<?> sendAppointmentRescheduleSms(
        String patientName,
        String phoneNumber,
        String doctorName,
        String appointmentDateTime,
        String appointmentId,
        String companyName,
        boolean rescheduled,
        boolean created,
        boolean cancel,
        boolean complete) {

        if (!smsEnabled) {
            logger.info("SMS service is disabled. Skipping appointment reminder SMS to {}", patientName);
            return baseResponse.successResponse("SMS service is disabled");
        }

        if (!isValidPhoneNumber(phoneNumber)) {
            logger.warn("Invalid phone number for patient {}: {}", patientName, phoneNumber);
            return baseResponse.errorResponse(HttpStatus.BAD_REQUEST, "Invalid phone number format");
        }

        String formattedPhone = formatPhoneNumber(phoneNumber);

        Map<String, Object> data = new HashMap<>();
        data.put("mobiles", country + formattedPhone);
        data.put("var1", appointmentDateTime);
        data.put("var2", companyName);
        data.put("var3", appointmentId);

        // Prepare request payload
        Map<String, Object> payload = new HashMap<>(6, 1);
        if (rescheduled) {
            payload.put("template_id", rescheduleSmsTemplateId);
        } else if (created) {
            payload.put("template_id", createSmsTemplateId);
        } else  if (cancel) {
            payload.put("template_id", cancelSmsTemplateId);
        } else if (complete) {
            payload.put("template_id", completeSmsTemplateId);
        } else {
            return baseResponse.errorResponse(HttpStatus.BAD_REQUEST, "Invalid Boolean Value");
        }
        payload.put("short_url", "");
        payload.put("short_url_expiry", ")");
        payload.put("realTimeResponse", "");
        payload.put("recipients", List.of(data));

        return sendSmsWithRetry(payload, 0);
    }

    @Override
    public boolean isSmsServiceEnabled() {
        return smsEnabled;
    }

    @Override
    public boolean isValidPhoneNumber(String phoneNumber) {
        if (phoneNumber == null || phoneNumber.trim().isEmpty()) {
            return false;
        }
        
        // Remove all non-digit characters
        String digitsOnly = phoneNumber.replaceAll("[^0-9]", "");
        
        // Indian phone number validation (10 digits)
        if (digitsOnly.length() == 10) {
            return digitsOnly.matches("[6-9][0-9]{9}"); // Indian mobile numbers start with 6-9
        }
        
        // International format with country code
        if (digitsOnly.length() >= 10 && digitsOnly.length() <= 15) {
            return digitsOnly.matches("[1-9][0-9]{9,14}");
        }
        
        return false;
    }

    private String formatPhoneNumber(String phoneNumber) {
        // Remove all non-digit characters except +
        String formatted = phoneNumber.replaceAll("[^0-9+]", "");
        
        // Handle Indian numbers
        if (formatted.startsWith("+91")) {
            formatted = formatted.substring(3); // Remove +91
        } else if (formatted.startsWith("91") && formatted.length() == 12) {
            formatted = formatted.substring(2); // Remove 91
        }
        
        return formatted;
    }

    private ResponseEntity<?> sendSmsWithRetry(Map<String, Object> payload, int attempt) {
        try (CloseableHttpClient httpClient = HttpClients.createDefault()) {
            HttpPost httpPost = new HttpPost(apiUrl);
            
            // Set headers
            httpPost.setHeader("authkey", authKey);
            httpPost.setHeader("Content-Type", "application/json");
            
            // Set payload
            String jsonPayload = objectMapper.writeValueAsString(payload);
            httpPost.setEntity(new StringEntity(jsonPayload));
            
            logger.debug("Sending SMS request (attempt {}): {}", attempt + 1, jsonPayload);
            
            try (CloseableHttpResponse response = httpClient.execute(httpPost)) {
                int statusCode = response.getCode();
                String responseBody = new String(response.getEntity().getContent().readAllBytes());
                
                logger.debug("MSG91 API Response - Status: {}, Body: {}", statusCode, responseBody);
                
                if (statusCode == 200) {
                    // Parse response
                    JsonNode responseJson = objectMapper.readTree(responseBody);
                    if (responseJson.has("type") && "success".equals(responseJson.get("type").asText())) {
                        logger.info("SMS sent successfully");
                        return baseResponse.successResponse("SMS sent successfully", responseJson);
                    } else {
                        String errorMessage = responseJson.has("message") ? 
                            responseJson.get("message").asText() : "Unknown error from MSG91 API";
                        logger.error("MSG91 API returned error: {}", errorMessage);
                        return baseResponse.errorResponse(HttpStatus.BAD_REQUEST, errorMessage);
                    }
                } else {
                    logger.error("MSG91 API request failed with status {}: {}", statusCode, responseBody);
                    
                    // Retry logic
                    if (attempt < maxRetryAttempts - 1) {
                        logger.info("Retrying SMS send in {} ms (attempt {}/{})", 
                            retryDelayMs, attempt + 2, maxRetryAttempts);
                        Thread.sleep(retryDelayMs);
                        return sendSmsWithRetry(payload, attempt + 1);
                    }
                    
                    return baseResponse.errorResponse(HttpStatus.BAD_REQUEST, 
                        "Failed to send SMS after " + maxRetryAttempts + " attempts");
                }
            }
        } catch (Exception e) {
            logger.error("Error sending SMS (attempt {}/{}): {}", attempt + 1, maxRetryAttempts, e.getMessage(), e);
            
            // Retry logic for exceptions
            if (attempt < maxRetryAttempts - 1) {
                try {
                    Thread.sleep(retryDelayMs);
                    return sendSmsWithRetry(payload, attempt + 1);
                } catch (InterruptedException ie) {
                    Thread.currentThread().interrupt();
                    return baseResponse.errorResponse(HttpStatus.INTERNAL_SERVER_ERROR, "SMS sending interrupted");
                }
            }
            
            return baseResponse.errorResponse(HttpStatus.INTERNAL_SERVER_ERROR, 
                "Failed to send SMS: " + e.getMessage());
        }
    }
}