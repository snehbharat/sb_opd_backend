package com.sbpl.OPD.Auth.utils;

import com.sbpl.OPD.Auth.model.User;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.security.Key;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

@Component
public class JwtService {

    private static final String AUTHORITIES_KEY = "roles";

    @Value("${jwt.secret-key}")
    private String base64Secret;

    @Value("${jwt.expiration-days}")
    private long accessTokenExpiryHours;

    @Value("${jwt.refresh-expiration-days}")
    private long refreshTokenExpiryHours;

    private Key signingKey;

    @PostConstruct
    public void init() {
        byte[] keyBytes = Decoders.BASE64.decode(base64Secret);
        this.signingKey = Keys.hmacShaKeyFor(keyBytes);
    }

    public String generateToken(UserDetails userDetails) {
        Map<String, Object> claims = new HashMap<>();
        if (userDetails instanceof User) {
            User user = (User) userDetails;
            claims.put("userId", user.getId());
            claims.put("role", user.getRole().name());
            // Removed permissions claim for pure RBAC
//            String permissions = user.getUserPermissions().stream()
//                    .map(up -> up.getPermission().getName())
//                    .collect(Collectors.joining(","));
//            claims.put("permissions", permissions);
        }
        return createToken(claims, userDetails.getUsername());
    }

    public String generateAccessToken(Authentication authentication, Map<String, Object> extraClaims) {
        return buildToken(
                authentication.getName(),
                authentication.getAuthorities(),
                extraClaims,
                TimeUnit.DAYS.toMillis(accessTokenExpiryHours)
        );
    }

    public String generateRefreshToken(Authentication authentication, Map<String, Object> extraClaims) {
        return buildToken(
                authentication.getName(),
                authentication.getAuthorities(),
                extraClaims,
                TimeUnit.DAYS.toMillis(refreshTokenExpiryHours)
        );
    }

    public Collection<GrantedAuthority> extractGrantedAuthorities(String token) {

        List<GrantedAuthority> authorities = new ArrayList<>();

        String roles = extractClaim(token, c -> c.get("roles", String.class));
        System.out.println("Roles from token: " + roles);
        String permissions = extractClaim(token, c -> c.get("permissions", String.class));
        System.out.println("Permissions from token: " + permissions);

        if (roles != null && !roles.isBlank()) {
            System.out.println("Processing roles: " + roles);
            String[] roleArray = roles.split(",");
            for (String role : roleArray) {
                String trimmedRole = role.trim();
                if (!trimmedRole.isBlank()) {
                    // If role doesn't already start with ROLE_, add the prefix
                    String finalRole;
                    if (!trimmedRole.startsWith("ROLE_")) {
                        finalRole = "ROLE_" + trimmedRole;
                    } else {
                        finalRole = trimmedRole;
                    }
                    System.out.println("Adding authority: " + finalRole);
                    authorities.add(new SimpleGrantedAuthority(finalRole));
                }
            }
        }

        // For pure RBAC, we don't process permissions from JWT token
        // All authorization is handled through roles
        
        System.out.println("Extracted authorities: " + authorities);
        return authorities;
    }

    private String buildToken(
            String subject,
            Collection<? extends GrantedAuthority> authorities,
            Map<String, Object> claims,
            long expiryMillis
    ) {

        // For pure RBAC, all authorities are roles
        Set<String> roles = new ArrayList<>(authorities).stream()
                .map(GrantedAuthority::getAuthority)
                .collect(Collectors.toSet());

        String rolesClaim = String.join(",", roles);
        String permissionsClaim = ""; // Empty for pure RBAC
        
        System.out.println("Building token with roles: " + roles);
        System.out.println("Roles claim: " + rolesClaim);

        String token = Jwts.builder()
                .subject(subject)
                .claim("roles", rolesClaim)
                .claim("permissions", permissionsClaim)
                .claims(claims)
                .issuedAt(new Date())
                .expiration(new Date(System.currentTimeMillis() + expiryMillis))
                .signWith(signingKey)
                .compact();
                
        System.out.println("Generated token: " + token);
        return token;
    }

    private String createToken(Map<String, Object> claims, String subject) {
        return Jwts.builder()
                .setClaims(claims)
                .setSubject(subject)
                .setIssuedAt(new Date(System.currentTimeMillis()))
                .setExpiration(new Date(System.currentTimeMillis() + TimeUnit.HOURS.toMillis(accessTokenExpiryHours)))
                .signWith(signingKey)
                .compact();
    }

    public Boolean validateToken(String token, UserDetails userDetails) {
        try {
            final String username = extractUsername(token);
            final String userDetailsUsername = userDetails.getUsername();
            boolean usernameMatch = username.equals(userDetailsUsername);
            boolean notExpired = !isTokenExpired(token);
            boolean isValid = usernameMatch && notExpired;
            
            System.out.println("Token validation details:");
            System.out.println("  Token username: " + username);
            System.out.println("  UserDetails username: " + userDetailsUsername);
            System.out.println("  Username match: " + usernameMatch);
            System.out.println("  Not expired: " + notExpired);
            System.out.println("  Overall valid: " + isValid);
            
            return isValid;
        } catch (Exception e) {
            System.out.println("Token validation failed with exception: " + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }

    public Long extractUserId(String token) {
        return extractClaim(token, claims -> claims.get("userId", Long.class));
    }

    public String extractRole(String token) {
        return extractClaim(token, claims -> claims.get("role", String.class));
    }

    // Removed extractPermissions method for pure RBAC
//    public List<String> extractPermissions(String token) {
//        String permissions = extractClaim(token, c -> c.get("permissions", String.class));
//        System.out.println(" i m from permissions on extracting : "+permissions);
//        if (permissions != null && !permissions.isBlank()) {
//            return Arrays.stream(permissions.split(","))
//                    .map(String::trim)
//                    .filter(p -> !p.isEmpty())
//                    .collect(Collectors.toList());
//        }
//        return new ArrayList<>();
//    }

    public boolean validateToken(String token) {
        try {
            parseClaims(token);
            return true;
        } catch (Exception ex) {
            return false;
        }
    }

    public String extractUsername(String token) {
        String username = extractClaim(token, Claims::getSubject);
        System.out.println("Extracted username from token: " + username);
        return username;
    }

    public Date extractExpiration(String token) {
        return extractClaim(token, Claims::getExpiration);
    }

    public List<String> extractAuthorities(String token) {
        String roles = extractClaim(token, claims -> claims.get(AUTHORITIES_KEY, String.class));
        return Arrays.asList(roles.split(","));
    }

    public <T> T extractClaim(String token, java.util.function.Function<Claims, T> resolver) {
        return resolver.apply(parseClaims(token));
    }

    public Map<String, Object> decodePayload(String token) {
        Claims claims = parseClaims(token);
        Map<String, Object> payload = new HashMap<>(claims);
        payload.remove(Claims.EXPIRATION);
        payload.remove(Claims.ISSUED_AT);
        payload.remove(Claims.SUBJECT);
        return payload;
    }

    private Claims parseClaims(String token) {
        return Jwts.parser()
                .verifyWith((SecretKey) signingKey)
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    private boolean isTokenExpired(String token) {
        return extractExpiration(token).before(new Date());
    }
}
