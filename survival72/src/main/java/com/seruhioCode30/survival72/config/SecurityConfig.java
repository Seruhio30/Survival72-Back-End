package com.seruhioCode30.survival72.config;

import com.seruhioCode30.survival72.config.properties.AdminSecurityProperties;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.session.ChangeSessionIdAuthenticationStrategy;
import org.springframework.security.web.authentication.session.SessionAuthenticationStrategy;
import org.springframework.security.web.csrf.HttpSessionCsrfTokenRepository;

@Configuration
public class SecurityConfig {

    private final AdminSecurityProperties adminSecurityProperties;

    public SecurityConfig(AdminSecurityProperties adminSecurityProperties) {
        this.adminSecurityProperties = adminSecurityProperties;
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public UserDetailsService adminUserDetailsService() {
        return username -> {
            String configuredUsername = adminSecurityProperties.getUsername();
            String configuredPasswordHash = adminSecurityProperties.getPasswordHash();

            if (configuredUsername == null
                    || configuredUsername.isBlank()
                    || configuredPasswordHash == null
                    || configuredPasswordHash.isBlank()
                    || !configuredUsername.equals(username)) {
                throw new UsernameNotFoundException(
                        "Admin credentials are not configured or invalid."
                );
            }

            return User.withUsername(configuredUsername)
                    .password(configuredPasswordHash)
                    .roles("ADMIN")
                    .build();
        };
    }

    @Bean
    public AuthenticationManager authenticationManager(
            AuthenticationConfiguration authenticationConfiguration
    ) throws Exception {
        return authenticationConfiguration.getAuthenticationManager();
    }

    @Bean
    public SessionAuthenticationStrategy sessionAuthenticationStrategy() {
        return new ChangeSessionIdAuthenticationStrategy();
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        HttpSessionCsrfTokenRepository csrfTokenRepository =
                new HttpSessionCsrfTokenRepository();

        http
                .cors(Customizer.withDefaults())
                .csrf(csrf -> csrf
                        .csrfTokenRepository(csrfTokenRepository)
                        .ignoringRequestMatchers(
                                "/api/join",
                                "/api/subscriptions/manage",
                                "/api/subscriptions/unsubscribe",
                                "/api/subscribers/**",
                                "/test-newsletter/**"
                        )
                )
                .authorizeHttpRequests(authorize -> authorize
                        .requestMatchers(
                                HttpMethod.POST,
                                "/api/admin/auth/login"
                        ).permitAll()
                        .requestMatchers(
                                HttpMethod.GET,
                                "/api/admin/auth/session"
                        ).permitAll()

                        .requestMatchers(HttpMethod.POST, "/api/join").permitAll()
                        .requestMatchers(
                                HttpMethod.GET,
                                "/api/subscriptions/manage"
                        ).permitAll()
                        .requestMatchers(
                                HttpMethod.PATCH,
                                "/api/subscriptions/manage"
                        ).permitAll()
                        .requestMatchers(
                                HttpMethod.POST,
                                "/api/subscriptions/unsubscribe"
                        ).permitAll()

                        .requestMatchers("/api/admin/**").authenticated()
                        .anyRequest().permitAll()
                )
                .formLogin(AbstractHttpConfigurer::disable)
                .httpBasic(AbstractHttpConfigurer::disable)
                .logout(AbstractHttpConfigurer::disable)
                .requestCache(AbstractHttpConfigurer::disable)
                .exceptionHandling(exceptionHandling -> exceptionHandling
                        .authenticationEntryPoint((request, response, exception) -> {
                            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                            response.setContentType("application/json");
                            response.getWriter().write(
                                    "{\"code\":\"ADMIN_AUTHENTICATION_REQUIRED\","
                                            + "\"message\":\"Administrator authentication is required.\"}"
                            );
                        })
                );

        return http.build();
    }
}
