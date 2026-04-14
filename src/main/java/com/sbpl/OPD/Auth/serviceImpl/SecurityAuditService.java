package com.sbpl.OPD.Auth.serviceImpl;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

@Service
public class SecurityAuditService {

    private static final Logger log = LoggerFactory.getLogger(SecurityAuditService.class);

    public void logAdminBypass(String action) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();

        if (auth == null) return;

        String username = auth.getName();
        String roles = auth.getAuthorities().toString();

        log.warn(
            "ADMIN BYPASS | user={} | roles={} | action={} | time={}",
            username,
            roles,
            action,
            LocalDateTime.now()
        );
    }
}
