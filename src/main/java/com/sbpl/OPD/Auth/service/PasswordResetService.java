package com.sbpl.OPD.Auth.service;

import com.sbpl.OPD.Auth.dto.ForgotPasswordRequestDto;
import com.sbpl.OPD.Auth.dto.ResetPasswordRequestDto;
import org.springframework.http.ResponseEntity;

public interface PasswordResetService {
    
    ResponseEntity<?> initiatePasswordReset(ForgotPasswordRequestDto request);
    
    ResponseEntity<?> resetPassword(ResetPasswordRequestDto request);
}