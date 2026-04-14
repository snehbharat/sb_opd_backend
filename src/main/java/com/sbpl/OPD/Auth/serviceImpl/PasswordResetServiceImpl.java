package com.sbpl.OPD.Auth.serviceImpl;

import com.sbpl.OPD.Auth.dto.ForgotPasswordRequestDto;
import com.sbpl.OPD.Auth.dto.ResetPasswordRequestDto;
import com.sbpl.OPD.Auth.model.PasswordResetToken;
import com.sbpl.OPD.Auth.model.User;
import com.sbpl.OPD.Auth.repository.PasswordResetTokenRepository;
import com.sbpl.OPD.Auth.repository.UserRepository;
import com.sbpl.OPD.Auth.service.PasswordResetService;
import com.sbpl.OPD.response.BaseResponse;
import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

@Service
public class PasswordResetServiceImpl implements PasswordResetService {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordResetTokenRepository tokenRepository;

    @Autowired
    private JavaMailSender mailSender;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private BaseResponse baseResponse;

    @Value("${app.base-url}")
    private String baseUrl;

    @Value("${app.mail.from}")
    private String fromEmail;

    @Value("${app.reset-token-expiry-hours}")
    private long expiryHours;

    @Override
    public ResponseEntity<?> initiatePasswordReset(ForgotPasswordRequestDto request) {
        Optional<User> userOptional = userRepository.findByEmail(request.getEmail());

        if (userOptional.isEmpty()) {
            // Return success even if user doesn't exist to prevent email enumeration
            return baseResponse.successResponse("If an account with that email exists, a password reset link has been sent.");
        }

        User user = userOptional.get();

        // Delete any existing tokens for this user
        tokenRepository.deleteByUser(user);

        // Generate new token
        String token = UUID.randomUUID().toString();
        PasswordResetToken resetToken = new PasswordResetToken();
        resetToken.setToken(token);
        resetToken.setUser(user);
        resetToken.setExpiryDate(LocalDateTime.now().plusHours(24)); // Token expires in 24 hours

        tokenRepository.save(resetToken);

        // Send email with reset link
        try {
            sendPasswordResetEmail(user, token);
            return baseResponse.successResponse("Password reset link has been sent to your email.");
        } catch (Exception e) {
            return baseResponse.errorResponse(HttpStatus.INTERNAL_SERVER_ERROR,
                    "Failed to send password reset email. Please try again later.");
        }
    }

    @Override
    public ResponseEntity<?> resetPassword(ResetPasswordRequestDto request) {
        Optional<PasswordResetToken> tokenOptional = tokenRepository.findByToken(request.getToken());

        if (tokenOptional.isEmpty()) {
            return baseResponse.errorResponse(HttpStatus.BAD_REQUEST, "Invalid or expired token.");
        }

        PasswordResetToken token = tokenOptional.get();

        if (token.isExpired() || token.getUsed()) {
            return baseResponse.errorResponse(HttpStatus.BAD_REQUEST, "Token has expired or already been used.");
        }

        User user = token.getUser();
        user.setPassword(passwordEncoder.encode(request.getNewPassword()));
        userRepository.save(user);

        // Mark token as used
        token.setUsed(true);
        tokenRepository.save(token);

        return baseResponse.successResponse("Password has been reset successfully.");
    }

    private void sendPasswordResetEmail(User user, String token) throws MessagingException {

        String resetLink = baseUrl + "/api/v1/auth/reset-password?token=" + token;

        MimeMessage mimeMessage = mailSender.createMimeMessage();
        MimeMessageHelper helper = new MimeMessageHelper(
                mimeMessage,
                MimeMessageHelper.MULTIPART_MODE_MIXED_RELATED,
                StandardCharsets.UTF_8.name()
        );

        helper.setTo(user.getEmail());
        helper.setFrom(fromEmail);
        helper.setSubject("🔐 Reset Your Password - HMS");

        helper.setText(buildResetEmailTemplate(user, resetLink), true);

        mailSender.send(mimeMessage);
    }


    private String buildResetEmailTemplate(User user, String resetLink) {

        return """
                <html>
                <body style="font-family: Arial, sans-serif; background-color:#f4f6f8; padding:20px;">
                    <div style="max-width:600px; margin:auto; background:white; padding:30px; border-radius:10px; box-shadow:0 2px 8px rgba(0,0,0,0.1);">
                
                        <h2 style="color:#2c3e50;">Password Reset Request</h2>
                
                        <p>Hello <strong>%s %s</strong>,</p>
                
                        <p>We received a request to reset your password.</p>
                
                        <div style="text-align:center; margin:30px 0;">
                            <a href="%s"
                               style="background-color:#007bff; color:white; padding:12px 25px;
                                      text-decoration:none; border-radius:5px; font-weight:bold;">
                                Reset Password
                            </a>
                        </div>
                
                        <p>This link will expire in <strong>%d hours</strong>.</p>
                
                        <p>If you did not request this, you can safely ignore this email.</p>
                
                        <hr style="margin:30px 0;">
                
                        <p style="font-size:12px; color:gray;">
                            HMIS Team<br/>
                            This is an automated message. Please do not reply.
                        </p>
                
                    </div>
                </body>
                </html>
                """.formatted(
                user.getFirstName(),
                user.getLastName(),
                resetLink,
                expiryHours
        );
    }
}
