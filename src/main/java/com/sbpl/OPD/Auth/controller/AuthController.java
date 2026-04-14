package com.sbpl.OPD.Auth.controller;

import com.sbpl.OPD.Auth.dto.ForgotPasswordRequestDto;
import com.sbpl.OPD.Auth.dto.LoginRequestDto;
import com.sbpl.OPD.Auth.dto.ResetPasswordRequestDto;
import com.sbpl.OPD.Auth.service.PasswordResetService;
import com.sbpl.OPD.Auth.service.UserService;
import com.sbpl.OPD.Auth.utils.AuthMetrics;
import com.sbpl.OPD.Auth.utils.JwtUtil;
import com.sbpl.OPD.dto.JwtResponse;
import com.sbpl.OPD.dto.UserDTO;
import com.sbpl.OPD.utils.RbacUtil;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api/v1/auth")
public class AuthController {

    @Autowired
    private JwtUtil jwtTokenUtil;

    @Autowired
    private UserService userService;

    @Autowired
    private AuthMetrics authMetrics;

    @Autowired
    private RbacUtil rbacUtil;


    @Autowired
    private PasswordResetService passwordResetService;

    @PostMapping("/login/with/password")
    public ResponseEntity<?> loginViaUserNameAndPassword(@Valid @RequestBody
                                                         LoginRequestDto logInViaUserNameAndPasswordRequestDto) {
        return userService.loginViaUsernameAndPassword(logInViaUserNameAndPasswordRequestDto);
    }

    @PostMapping("/register")
    public ResponseEntity<?> registerUser(@Valid @RequestBody UserDTO userDTO) {
            if (rbacUtil.isSuperAdmin()) {
                return userService.createUser1(userDTO);
            } else {
                return userService.createUser(userDTO);
            }
    }

    @PostMapping("/forgot-password")
    public ResponseEntity<?> forgotPassword(@Valid @RequestBody ForgotPasswordRequestDto request) {
        return passwordResetService.initiatePasswordReset(request);
    }

    @PostMapping("/reset-password")
    public ResponseEntity<?> resetPassword(@Valid @RequestBody ResetPasswordRequestDto request) {
        return passwordResetService.resetPassword(request);
    }

    @PostMapping("/refresh")
    public ResponseEntity<?> refreshToken(HttpServletRequest request) {
        String authHeader = request.getHeader("Authorization");

        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            String token = authHeader.substring(7);

            if (jwtTokenUtil.isTokenValid(token)) {
                String username = jwtTokenUtil.extractUsername(token);

                try {
                    UserDetails userDetails = userService.loadUserByUsername(username);

                    // Generate new token with fresh expiration
                    String newToken = jwtTokenUtil.generateToken(userDetails);

                    UserDTO userDTO = userService.findByUsername(username);

                    return ResponseEntity.ok(new JwtResponse(
                            newToken,
                            "Bearer",
                            userDTO.getId(),
                            userDTO.getUsername(),
                            userDTO.getEmail(),
                            userDTO.getRole().toString()
                    ));
                } catch (Exception e) {
                    return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                            .body(Map.of("error", "Invalid token"));
                }
            }
        }

        return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                .body(Map.of("error", "Invalid token"));
    }
}