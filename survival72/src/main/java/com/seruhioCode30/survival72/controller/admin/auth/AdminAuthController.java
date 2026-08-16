package com.seruhioCode30.survival72.controller.admin.auth;

import com.seruhioCode30.survival72.controller.admin.auth.dto.AdminAuthResponse;
import com.seruhioCode30.survival72.controller.admin.auth.dto.AdminLoginRequest;
import com.seruhioCode30.survival72.controller.admin.auth.dto.AdminSessionResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.session.SessionAuthenticationStrategy;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;
import org.springframework.security.web.context.SecurityContextRepository;
import org.springframework.security.web.csrf.CsrfToken;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/admin/auth")
public class AdminAuthController {

    private final AuthenticationManager authenticationManager;
    private final SessionAuthenticationStrategy sessionAuthenticationStrategy;
    private final SecurityContextRepository securityContextRepository =
            new HttpSessionSecurityContextRepository();

    public AdminAuthController(
            AuthenticationManager authenticationManager,
            SessionAuthenticationStrategy sessionAuthenticationStrategy
    ) {
        this.authenticationManager = authenticationManager;
        this.sessionAuthenticationStrategy = sessionAuthenticationStrategy;
    }

    @PostMapping("/login")
    public ResponseEntity<AdminAuthResponse> login(
            @Valid @RequestBody AdminLoginRequest request,
            HttpServletRequest httpRequest,
            HttpServletResponse httpResponse
    ) {
        Authentication authentication = authenticationManager.authenticate(
                UsernamePasswordAuthenticationToken.unauthenticated(
                        request.username(),
                        request.password()
                )
        );

        sessionAuthenticationStrategy.onAuthentication(
                authentication,
                httpRequest,
                httpResponse
        );

        SecurityContext securityContext =
                SecurityContextHolder.createEmptyContext();

        securityContext.setAuthentication(authentication);
        SecurityContextHolder.setContext(securityContext);

        securityContextRepository.saveContext(
                securityContext,
                httpRequest,
                httpResponse
        );

        return ResponseEntity.ok(
                new AdminAuthResponse(
                        "AUTHENTICATED",
                        "Administrator authenticated.",
                        true
                )
        );
    }

    @GetMapping("/session")
    public ResponseEntity<AdminSessionResponse> session(
            Authentication authentication,
            CsrfToken csrfToken
    ) {
        boolean authenticated =
                authentication != null
                        && authentication.isAuthenticated()
                        && !(authentication instanceof AnonymousAuthenticationToken);

        String username = authenticated
                ? authentication.getName()
                : null;

        return ResponseEntity.ok(
                new AdminSessionResponse(
                        authenticated,
                        username,
                        csrfToken.getToken(),
                        csrfToken.getHeaderName()
                )
        );
    }

    @PostMapping("/logout")
    public ResponseEntity<AdminAuthResponse> logout(
            HttpServletRequest request
    ) {
        HttpSession session = request.getSession(false);

        if (session != null) {
            session.invalidate();
        }

        SecurityContextHolder.clearContext();

        return ResponseEntity.ok(
                new AdminAuthResponse(
                        "LOGGED_OUT",
                        "Administrator session ended.",
                        false
                )
        );
    }
}
