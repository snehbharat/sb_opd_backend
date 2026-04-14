package com.sbpl.OPD.response;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;

@Component
public class BaseResponse {

    public ResponseEntity<ResponseDto> successResponse(String message) {
        ResponseDto response = new ResponseDto();
        response.setResponse(true);
        response.setMessage(message);
        response.setStatus(HttpStatus.OK);
        response.setTimestamp(java.time.LocalDateTime.now());
        return ResponseEntity.ok(response);
    }

    public ResponseEntity<ResponseDto> successResponse(String message, Object data) {
        ResponseDto response = new ResponseDto();
        response.setResponse(true);
        response.setMessage(message);
        response.setStatus(HttpStatus.OK);
        response.setData(data);
        response.setTimestamp(java.time.LocalDateTime.now());
        return ResponseEntity.ok(response);
    }

    public ResponseEntity<ResponseDto> errorResponse(HttpStatus status, String message) {
        ResponseDto response = new ResponseDto();
        response.setResponse(false);
        response.setStatus(status);
        response.setMessage(message);
        response.setTimestamp(java.time.LocalDateTime.now());
        return ResponseEntity.status(status).body(response);
    }

    public ResponseEntity<ResponseDto> errorResponse(HttpStatus status, String message, Object data) {
        ResponseDto response = new ResponseDto();
        response.setResponse(false);
        response.setStatus(status);
        response.setMessage(message);
        response.setData(data);
        response.setTimestamp(java.time.LocalDateTime.now());
        return ResponseEntity.status(status).body(response);
    }
}
