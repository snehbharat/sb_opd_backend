package com.sbpl.OPD.Auth.config;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Lazy;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.Arrays;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity(prePostEnabled = false) // We'll rely on manual checks in controllers
public class SecurityConfig {

    @Autowired
    @Lazy
    private JwtAuthenticationEntryPoint jwtAuthenticationEntryPoint;

    @Autowired
    @Lazy
    private JwtRequestFilter jwtRequestFilter;

//    @Autowired
//    private CachedUserDetailsService cachedUserDetailsService;

    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration config) throws Exception {
        return config.getAuthenticationManager();
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder(); // Increased strength for better security
    }

//    @Bean
//    public UserDetailsService userDetailsService() {
//        return cachedUserDetailsService;
//    }

   @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {

        http
                .csrf(AbstractHttpConfigurer::disable)
                .authorizeHttpRequests(auth -> auth

                        .requestMatchers("/api/v1/auth/login").permitAll()
                        .requestMatchers("/api/v1/auth/register").permitAll()
                        .requestMatchers("/api/v1/auth/refresh").permitAll()
                        .requestMatchers("/api/v1/health-check/**").permitAll()
                        .requestMatchers("/api/v1/public/**").permitAll()
                        .requestMatchers("/api/v1/auth/login/with/password").permitAll()
                        .requestMatchers("/api/v1/enums/**").permitAll()
                        .requestMatchers("/api/test/**").permitAll()

                        .requestMatchers("/api/v1/users/create").hasAnyRole("SUPER_ADMIN", "SUPER_ADMIN_MANAGER", "SAAS_ADMIN", "SAAS_ADMIN_MANAGER", "BRANCH_MANAGER")
                        .requestMatchers("/api/v1/users/update/**").hasAnyRole("SUPER_ADMIN", "SUPER_ADMIN_MANAGER", "SAAS_ADMIN", "SAAS_ADMIN_MANAGER", "BRANCH_MANAGER")
                        .requestMatchers("/api/v1/users/delete/**").hasAnyRole("SUPER_ADMIN", "SUPER_ADMIN_MANAGER", "SAAS_ADMIN", "SAAS_ADMIN_MANAGER", "BRANCH_MANAGER")
                        .requestMatchers("/api/v1/users/logged-in/user-details").authenticated()
                        .requestMatchers("/api/v1/users/**").authenticated()

                        .requestMatchers("/api/v1/company-profiles/**")
                        .hasAnyRole("SAAS_ADMIN_MANAGER", "SAAS_ADMIN", "SUPER_ADMIN", "SUPER_ADMIN_MANAGER","BRANCH_MANAGER","RECEPTIONIST","PATIENT","BILLING_STAFF","DOCTOR")

//                        .requestMatchers("/api/v1/branches/**").hasAnyRole("SAAS_ADMIN_MANAGER", "SAAS_ADMIN", "SUPER_ADMIN", "SUPER_ADMIN_MANAGER")
                        .requestMatchers("/api/v1/branches/**").fullyAuthenticated()

                        .requestMatchers("/api/v1/departments/**").authenticated()

                        .requestMatchers("/api/v1/user-assignment/**").hasAnyRole("SAAS_ADMIN_MANAGER", "SAAS_ADMIN", "SUPER_ADMIN", "SUPER_ADMIN_MANAGER", "BRANCH_MANAGER")

                        .requestMatchers("/api/v1/doctors/create").hasAnyRole("SAAS_ADMIN_MANAGER", "SAAS_ADMIN", "SUPER_ADMIN", "SUPER_ADMIN_MANAGER", "BRANCH_MANAGER", "STAFF")
                        .requestMatchers("/api/v1/doctors/update/**").hasAnyRole("SAAS_ADMIN_MANAGER", "SAAS_ADMIN", "SUPER_ADMIN", "SUPER_ADMIN_MANAGER", "BRANCH_MANAGER", "STAFF")
                        .requestMatchers("/api/v1/doctors/delete/**").hasAnyRole("SAAS_ADMIN_MANAGER", "SAAS_ADMIN", "SUPER_ADMIN", "SUPER_ADMIN_MANAGER", "BRANCH_MANAGER", "STAFF")

                        .requestMatchers("/api/v1/customers/create").hasAnyRole("SAAS_ADMIN_MANAGER", "BRANCH_MANAGER", "SAAS_ADMIN", "SUPER_ADMIN", "SUPER_ADMIN_MANAGER", "DOCTOR", "STAFF", "RECEPTIONIST", "BILLING_STAFF")
                        .requestMatchers("/api/v1/customers/**").hasAnyRole("RECEPTIONIST", "STAFF", "DOCTOR", "SAAS_ADMIN_MANAGER", "BRANCH_MANAGER", "SAAS_ADMIN", "SUPER_ADMIN", "SUPER_ADMIN_MANAGER")
                        .requestMatchers("/api/v1/customers/update/**").hasAnyRole("SAAS_ADMIN_MANAGER", "BRANCH_MANAGER", "SAAS_ADMIN", "SUPER_ADMIN", "SUPER_ADMIN_MANAGER", "DOCTOR", "STAFF", "RECEPTIONIST", "BILLING_STAFF")
                        .requestMatchers("/api/v1/customers/delete/**").hasAnyRole("SAAS_ADMIN_MANAGER", "BRANCH_MANAGER", "SAAS_ADMIN", "SUPER_ADMIN", "SUPER_ADMIN_MANAGER")
                        .requestMatchers("/api/v1/customers/phone").hasAnyRole("SAAS_ADMIN_MANAGER", "BRANCH_MANAGER", "SAAS_ADMIN", "SUPER_ADMIN", "SUPER_ADMIN_MANAGER", "DOCTOR", "STAFF", "RECEPTIONIST", "BILLING_STAFF")
                        .requestMatchers("/api/v1/customers/by-email").hasAnyRole("SAAS_ADMIN_MANAGER", "BRANCH_MANAGER", "SAAS_ADMIN", "SUPER_ADMIN", "SUPER_ADMIN_MANAGER", "DOCTOR", "STAFF", "RECEPTIONIST", "BILLING_STAFF")

                        .requestMatchers("/api/v1/doctors/my").authenticated()
                        .requestMatchers("/api/v1/doctors").hasAnyRole("BILLING_STAFF", "RECEPTIONIST", "DOCTOR", "SAAS_ADMIN_MANAGER", "BRANCH_MANAGER", "SAAS_ADMIN", "SUPER_ADMIN", "SUPER_ADMIN_MANAGER")
                        .requestMatchers("/api/v1/doctors/**").hasAnyRole("BILLING_STAFF", "RECEPTIONIST", "DOCTOR", "SAAS_ADMIN_MANAGER", "BRANCH_MANAGER", "SAAS_ADMIN", "SUPER_ADMIN", "SUPER_ADMIN_MANAGER")

                        .requestMatchers("/api/v1/medical-records/my").authenticated()
                        .requestMatchers("/api/v1/medical-records/upload").hasAnyRole("DOCTOR", "SAAS_ADMIN_MANAGER", "BRANCH_MANAGER", "SAAS_ADMIN", "SUPER_ADMIN", "SUPER_ADMIN_MANAGER", "BILLING_STAFF", "RECEPTIONIST", "PATIENT")
                        .requestMatchers("/api/v1/medical-records/**").hasAnyRole("DOCTOR", "SAAS_ADMIN_MANAGER", "BRANCH_MANAGER", "SAAS_ADMIN", "SUPER_ADMIN", "SUPER_ADMIN_MANAGER", "BILLING_STAFF", "RECEPTIONIST", "PATIENT")

                        .requestMatchers("/api/v1/appointments/create").hasAnyRole("DOCTOR", "SAAS_ADMIN_MANAGER", "BRANCH_MANAGER", "SAAS_ADMIN", "SUPER_ADMIN", "SUPER_ADMIN_MANAGER", "RECEPTIONIST", "STAFF", "BILLING_STAFF")
                        .requestMatchers("/api/v1/appointments/update/**").hasAnyRole("DOCTOR", "SAAS_ADMIN_MANAGER", "BRANCH_MANAGER", "SAAS_ADMIN", "SUPER_ADMIN", "SUPER_ADMIN_MANAGER", "RECEPTIONIST", "STAFF", "BILLING_STAFF")
                        .requestMatchers("/api/v1/appointments/delete/**").hasAnyRole("DOCTOR", "SAAS_ADMIN_MANAGER", "BRANCH_MANAGER", "SAAS_ADMIN", "SUPER_ADMIN", "SUPER_ADMIN_MANAGER", "RECEPTIONIST", "STAFF", "BILLING_STAFF")
                        .requestMatchers("/api/v1/appointments/status/**").hasAnyRole("DOCTOR", "SAAS_ADMIN_MANAGER", "BRANCH_MANAGER", "SAAS_ADMIN", "SUPER_ADMIN", "SUPER_ADMIN_MANAGER", "RECEPTIONIST", "STAFF", "BILLING_STAFF")
                        .requestMatchers("/api/v1/appointments/patient/**").hasAnyRole("DOCTOR", "SAAS_ADMIN_MANAGER", "BRANCH_MANAGER", "SAAS_ADMIN", "SUPER_ADMIN", "SUPER_ADMIN_MANAGER", "RECEPTIONIST", "STAFF", "BILLING_STAFF")
                        .requestMatchers("/api/v1/appointments/doctor/**").hasAnyRole("DOCTOR", "SAAS_ADMIN_MANAGER", "BRANCH_MANAGER", "SAAS_ADMIN", "SUPER_ADMIN", "SUPER_ADMIN_MANAGER", "RECEPTIONIST", "STAFF", "BILLING_STAFF")
                        

                        .requestMatchers("/api/v1/appointments/add-vitals").hasAnyRole("DOCTOR", "SAAS_ADMIN_MANAGER", "BRANCH_MANAGER", "SAAS_ADMIN", "SUPER_ADMIN", "SUPER_ADMIN_MANAGER", "BILLING_STAFF", "RECEPTIONIST")
                        .requestMatchers("/api/v1/appointments/vitals").hasAnyRole("DOCTOR", "SAAS_ADMIN_MANAGER", "BRANCH_MANAGER", "SAAS_ADMIN", "SUPER_ADMIN", "SUPER_ADMIN_MANAGER", "BILLING_STAFF", "RECEPTIONIST")

                        .requestMatchers("/api/v1/appointments/raise-invoice").hasAnyRole("DOCTOR", "SAAS_ADMIN_MANAGER", "BRANCH_MANAGER", "SAAS_ADMIN", "SUPER_ADMIN", "SUPER_ADMIN_MANAGER", "BILLING_STAFF", "RECEPTIONIST")
                        .requestMatchers("/api/v1/appointments/create-with-slot").hasAnyRole("DOCTOR", "SAAS_ADMIN_MANAGER", "BRANCH_MANAGER", "SAAS_ADMIN", "SUPER_ADMIN", "SUPER_ADMIN_MANAGER", "RECEPTIONIST", "STAFF", "BILLING_STAFF")
                        .requestMatchers("/api/v1/appointments/populate-company-info").hasAnyRole("SAAS_ADMIN_MANAGER", "BRANCH_MANAGER", "SAAS_ADMIN", "SUPER_ADMIN", "SUPER_ADMIN_MANAGER")
                        .requestMatchers("/api/v1/appointments/*/follow-up").hasAnyRole("DOCTOR", "SAAS_ADMIN_MANAGER", "BRANCH_MANAGER", "SAAS_ADMIN", "SUPER_ADMIN", "SUPER_ADMIN_MANAGER", "RECEPTIONIST", "STAFF", "BILLING_STAFF")
                        .requestMatchers("/api/v1/appointments/*/follow-ups").hasAnyRole("DOCTOR", "SAAS_ADMIN_MANAGER", "BRANCH_MANAGER", "SAAS_ADMIN", "SUPER_ADMIN", "SUPER_ADMIN_MANAGER", "RECEPTIONIST", "STAFF", "BILLING_STAFF")
                        .requestMatchers("/api/v1/appointments/*/mark-follow-up-required").hasAnyRole("DOCTOR", "SAAS_ADMIN_MANAGER", "BRANCH_MANAGER", "SAAS_ADMIN", "SUPER_ADMIN", "SUPER_ADMIN_MANAGER", "RECEPTIONIST", "STAFF", "BILLING_STAFF")
                        .requestMatchers("/api/v1/appointments/calendar-view").hasAnyRole("DOCTOR", "SAAS_ADMIN_MANAGER", "BRANCH_MANAGER", "SAAS_ADMIN", "SUPER_ADMIN", "SUPER_ADMIN_MANAGER", "RECEPTIONIST", "STAFF", "BILLING_STAFF")
                        .requestMatchers("/api/v1/appointments/calendar-view/range").hasAnyRole("DOCTOR", "SAAS_ADMIN_MANAGER", "BRANCH_MANAGER", "SAAS_ADMIN", "SUPER_ADMIN", "SUPER_ADMIN_MANAGER", "RECEPTIONIST", "STAFF", "BILLING_STAFF")
                        .requestMatchers("/api/v1/appointments/*/mark-no-show").hasAnyRole("DOCTOR", "SAAS_ADMIN_MANAGER", "BRANCH_MANAGER", "SAAS_ADMIN", "SUPER_ADMIN", "SUPER_ADMIN_MANAGER", "RECEPTIONIST", "STAFF", "BILLING_STAFF")
                        .requestMatchers("/api/v1/appointments/no-show").hasAnyRole("DOCTOR", "SAAS_ADMIN_MANAGER", "BRANCH_MANAGER", "SAAS_ADMIN", "SUPER_ADMIN", "SUPER_ADMIN_MANAGER", "RECEPTIONIST", "STAFF", "BILLING_STAFF")
                        .requestMatchers("/api/v1/appointments/no-show/doctor/*").hasAnyRole("DOCTOR", "SAAS_ADMIN_MANAGER", "BRANCH_MANAGER", "SAAS_ADMIN", "SUPER_ADMIN", "SUPER_ADMIN_MANAGER", "RECEPTIONIST", "STAFF", "BILLING_STAFF")
                        .requestMatchers("/api/v1/appointments/no-show/patient/*").hasAnyRole("DOCTOR", "SAAS_ADMIN_MANAGER", "BRANCH_MANAGER", "SAAS_ADMIN", "SUPER_ADMIN", "SUPER_ADMIN_MANAGER", "RECEPTIONIST", "STAFF", "BILLING_STAFF")

                        .requestMatchers("/api/v1/bills/all").hasAnyRole("BILLING_STAFF", "BRANCH_MANAGER", "SAAS_ADMIN_MANAGER", "SAAS_ADMIN", "SUPER_ADMIN", "SUPER_ADMIN_MANAGER","RECEPTIONIST")
                        .requestMatchers("/api/v1/bills/{id}").hasAnyRole("BILLING_STAFF", "RECEPTIONIST", "BRANCH_MANAGER", "SAAS_ADMIN_MANAGER", "SAAS_ADMIN", "SUPER_ADMIN", "SUPER_ADMIN_MANAGER")
                        .requestMatchers("/api/v1/bills/**").hasAnyRole("BILLING_STAFF", "RECEPTIONIST", "BRANCH_MANAGER", "SAAS_ADMIN_MANAGER", "SAAS_ADMIN", "SUPER_ADMIN", "SUPER_ADMIN_MANAGER")
                        .requestMatchers("/api/v1/bills/create").hasAnyRole("BILLING_STAFF", "RECEPTIONIST", "BRANCH_MANAGER", "SAAS_ADMIN_MANAGER", "SAAS_ADMIN", "SUPER_ADMIN", "SUPER_ADMIN_MANAGER")
                        .requestMatchers("/api/v1/bills/update/**").hasAnyRole("BILLING_STAFF", "RECEPTIONIST", "BRANCH_MANAGER", "SAAS_ADMIN_MANAGER", "SAAS_ADMIN", "SUPER_ADMIN", "SUPER_ADMIN_MANAGER")
                        .requestMatchers("/api/v1/bills/delete/**").hasAnyRole("BILLING_STAFF", "RECEPTIONIST", "BRANCH_MANAGER", "SAAS_ADMIN_MANAGER", "SAAS_ADMIN", "SUPER_ADMIN", "SUPER_ADMIN_MANAGER")
                        .requestMatchers("/api/v1/bills/patpleazient/**").hasAnyRole("BILLING_STAFF", "RECEPTIONIST", "BRANCH_MANAGER", "SAAS_ADMIN_MANAGER", "SAAS_ADMIN", "SUPER_ADMIN", "SUPER_ADMIN_MANAGER")
                        .requestMatchers("/api/v1/bills/staff/**").hasAnyRole("BILLING_STAFF", "RECEPTIONIST", "BRANCH_MANAGER", "SAAS_ADMIN_MANAGER", "SAAS_ADMIN", "SUPER_ADMIN", "SUPER_ADMIN_MANAGER")
                        .requestMatchers("/api/v1/bills/status/**").hasAnyRole("BILLING_STAFF", "RECEPTIONIST", "BRANCH_MANAGER", "SAAS_ADMIN_MANAGER", "SAAS_ADMIN", "SUPER_ADMIN", "SUPER_ADMIN_MANAGER")
                        .requestMatchers("/api/v1/bills/{id}/payment").hasAnyRole("BILLING_STAFF", "RECEPTIONIST", "BRANCH_MANAGER", "SAAS_ADMIN_MANAGER", "SAAS_ADMIN", "SUPER_ADMIN", "SUPER_ADMIN_MANAGER")
                        .requestMatchers("/api/v1/bills/my").authenticated()
                        .requestMatchers("/api/v1/bills/my/**").hasAnyRole("BILLING_STAFF", "RECEPTIONIST", "BRANCH_MANAGER", "SAAS_ADMIN_MANAGER", "SAAS_ADMIN", "SUPER_ADMIN", "SUPER_ADMIN_MANAGER")
                        .requestMatchers("/api/v1/bills/{id}/receipt").hasAnyRole("BILLING_STAFF", "RECEPTIONIST", "BRANCH_MANAGER", "SAAS_ADMIN_MANAGER", "SAAS_ADMIN", "SUPER_ADMIN", "SUPER_ADMIN_MANAGER")
                        .requestMatchers("/api/v1/bills/{id}/formatted-receipt").hasAnyRole("BILLING_STAFF", "RECEPTIONIST", "BRANCH_MANAGER", "SAAS_ADMIN_MANAGER", "SAAS_ADMIN", "SUPER_ADMIN", "SUPER_ADMIN_MANAGER")
                        .requestMatchers("/api/v1/billing-staff/dashboard/**").hasAnyRole("BILLING_STAFF", "RECEPTIONIST", "STAFF", "BRANCH_MANAGER", "SAAS_ADMIN_MANAGER", "SAAS_ADMIN", "SUPER_ADMIN", "SUPER_ADMIN_MANAGER")
                        .requestMatchers("/api/v1/bills/top-performing-staff").hasAnyRole("BILLING_STAFF", "RECEPTIONIST", "STAFF", "BRANCH_MANAGER", "SAAS_ADMIN_MANAGER", "SAAS_ADMIN", "SUPER_ADMIN", "SUPER_ADMIN_MANAGER")

                        .requestMatchers("/api/v1/appointments").hasAnyRole("BILLING_STAFF", "RECEPTIONIST", "DOCTOR", "SAAS_ADMIN_MANAGER", "BRANCH_MANAGER", "SAAS_ADMIN", "SUPER_ADMIN", "SUPER_ADMIN_MANAGER", "STAFF")
                        .requestMatchers("/api/v1/appointments/**").hasAnyRole("BILLING_STAFF", "RECEPTIONIST", "DOCTOR", "SAAS_ADMIN_MANAGER", "BRANCH_MANAGER", "SAAS_ADMIN", "SUPER_ADMIN", "SUPER_ADMIN_MANAGER", "STAFF")

                        .requestMatchers("/api/v1/customers/my").authenticated()
                        .requestMatchers("/api/v1/customers").hasAnyRole("BILLING_STAFF", "RECEPTIONIST", "STAFF", "DOCTOR", "SAAS_ADMIN_MANAGER", "BRANCH_MANAGER", "SAAS_ADMIN", "SUPER_ADMIN", "SUPER_ADMIN_MANAGER")
                        .requestMatchers("/api/v1/customers/**").hasAnyRole("BILLING_STAFF", "RECEPTIONIST", "STAFF", "DOCTOR", "SAAS_ADMIN_MANAGER", "BRANCH_MANAGER", "SAAS_ADMIN", "SUPER_ADMIN", "SUPER_ADMIN_MANAGER")

                        .requestMatchers("/api/v1/appointments/my").authenticated()
                        .requestMatchers("/api/v1/appointments/my/**").hasAnyRole("DOCTOR", "BRANCH_MANAGER", "SAAS_ADMIN", "SUPER_ADMIN", "SUPER_ADMIN_MANAGER", "RECEPTIONIST", "STAFF", "PATIENT", "BILLING_STAFF")
                        .requestMatchers("/api/v1/customers/my/**").hasAnyRole("DOCTOR", "BRANCH_MANAGER", "SAAS_ADMIN", "SUPER_ADMIN", "SUPER_ADMIN_MANAGER", "RECEPTIONIST", "STAFF", "PATIENT", "BILLING_STAFF")

                        .requestMatchers("/api/v1/schedules/**").hasAnyRole("RECEPTIONIST", "DOCTOR", "SAAS_ADMIN_MANAGER", "BRANCH_MANAGER", "SAAS_ADMIN", "SUPER_ADMIN", "SUPER_ADMIN_MANAGER", "BILLING_STAFF")

                        .requestMatchers("/api/v1/receptionist/analytics/**").hasAnyRole("RECEPTIONIST", "STAFF", "BRANCH_MANAGER", "SAAS_ADMIN", "SUPER_ADMIN", "SUPER_ADMIN_MANAGER")
                        
                        .requestMatchers("/api/v1/doctors/analytics/**").hasAnyRole("DOCTOR", "BRANCH_MANAGER", "SAAS_ADMIN", "SUPER_ADMIN", "SUPER_ADMIN_MANAGER")
                        
                        .requestMatchers("/api/branchmanager/analytics/**").hasAnyRole("BRANCH_MANAGER", "SAAS_ADMIN", "SUPER_ADMIN", "SUPER_ADMIN_MANAGER")
                        
                        .requestMatchers("/api/saasadmin/analytics/**").hasAnyRole("SAAS_ADMIN", "SUPER_ADMIN", "SUPER_ADMIN_MANAGER")
                        
                        .requestMatchers("/api/superadmin/analytics/**").hasAnyRole("SUPER_ADMIN", "SUPER_ADMIN_MANAGER")
                        
                        .requestMatchers("/api/v1/patient/dashboard").authenticated()
                        .requestMatchers("/api/patient/dashboard/**").hasAnyRole("PATIENT", "DOCTOR", "BRANCH_MANAGER", "SAAS_ADMIN", "SUPER_ADMIN", "SUPER_ADMIN_MANAGER")
                        .requestMatchers("/api/v1/coupons/**").fullyAuthenticated()
                        .requestMatchers("/api/v1/treatments/**").fullyAuthenticated()

                        .anyRequest().fullyAuthenticated()
                )
                .exceptionHandling(ex ->
                        ex.authenticationEntryPoint(jwtAuthenticationEntryPoint)
                )
                .sessionManagement(sess ->
                        sess.sessionCreationPolicy(SessionCreationPolicy.STATELESS)
                )
                .cors(cors -> cors.configurationSource(corsConfigurationSource()));

        http.addFilterBefore(
                jwtRequestFilter,
                UsernamePasswordAuthenticationFilter.class
        );

        return http.build();
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();
        configuration.setAllowedOriginPatterns(Arrays.asList("*"));
        configuration.setAllowedMethods(Arrays.asList("GET", "POST", "PUT", "DELETE", "OPTIONS", "HEAD"));
        configuration.setAllowedHeaders(Arrays.asList("*"));
        configuration.setAllowCredentials(true);
        configuration.setMaxAge(3600L);
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        return source;
    }
}