package com.seruhioCode30.survival72.security;

import com.seruhioCode30.survival72.config.SecurityConfig;
import com.seruhioCode30.survival72.config.properties.AdminSecurityProperties;
import com.seruhioCode30.survival72.controller.admin.AdminSecurityProbeController;
import com.seruhioCode30.survival72.controller.admin.auth.AdminAuthController;
import com.seruhioCode30.survival72.controller.admin.auth.AdminAuthExceptionHandler;
import com.seruhioCode30.survival72.controller.admin.subscriber.AdminSubscriberController;
import com.seruhioCode30.survival72.controller.admin.subscriber.AdminSubscriberExceptionHandler;
import com.seruhioCode30.survival72.controller.admin.subscriber.dto.AdminSubscriberPageResponse;
import com.seruhioCode30.survival72.service.admin.subscriber.AdminSubscriberService;
import com.seruhioCode30.survival72.controller.join.JoinController;
import com.seruhioCode30.survival72.controller.subscription.SubscriptionManagementController;
import com.seruhioCode30.survival72.controller.subscription.SubscriptionUnsubscribeController;
import com.seruhioCode30.survival72.model.SubscriberPreference;
import com.seruhioCode30.survival72.service.join.JoinApplicationService;
import com.seruhioCode30.survival72.service.join.JoinOutcome;
import com.seruhioCode30.survival72.service.join.JoinResult;
import com.seruhioCode30.survival72.service.subscription.SubscriptionManagementService;
import com.seruhioCode30.survival72.service.subscription.SubscriptionManagementView;
import com.seruhioCode30.survival72.service.subscription.SubscriptionUnsubscribeApplicationService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = {
        AdminAuthController.class,
        AdminSecurityProbeController.class,
        JoinController.class,
        SubscriptionManagementController.class,
        SubscriptionUnsubscribeController.class,
        AdminSubscriberController.class
})
@Import({
        SecurityConfig.class,
        AdminAuthExceptionHandler.class,
        AdminSubscriberExceptionHandler.class
})
@EnableConfigurationProperties(AdminSecurityProperties.class)
class AdminSecurityTests {

    private static final String ADMIN_USERNAME = "test-admin";
    private static final String ADMIN_PASSWORD = "test-password";
    private static final String MANAGEMENT_TOKEN = "valid-management-token";

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private JoinApplicationService joinApplicationService;

    @MockBean
    private SubscriptionManagementService subscriptionManagementService;

    @MockBean
    private SubscriptionUnsubscribeApplicationService unsubscribeApplicationService;

    @MockBean
    private AdminSubscriberService adminSubscriberService;

    @DynamicPropertySource
    static void adminProperties(DynamicPropertyRegistry registry) {
        registry.add("app.admin.username", () -> ADMIN_USERNAME);
        registry.add(
                "app.admin.password-hash",
                () -> new BCryptPasswordEncoder().encode(ADMIN_PASSWORD)
        );
    }

    @Test
    void authenticatedAdminSessionCanReadSubscribers() throws Exception {
        MockHttpSession session = authenticatedSession();

        when(adminSubscriberService.findSubscribers(0, 20, null, null))
                .thenReturn(new AdminSubscriberPageResponse(
                        java.util.List.of(),
                        0,
                        20,
                        0,
                        0,
                        false
                ));

        mockMvc.perform(get("/api/admin/subscribers")
                        .session(session))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.page").value(0))
                .andExpect(jsonPath("$.size").value(20));
    }

    @Test
    void adminRouteWithoutSessionIsBlocked() throws Exception {
        mockMvc.perform(get("/api/admin/security-check"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code")
                        .value("ADMIN_AUTHENTICATION_REQUIRED"));
    }

    @Test
    void validLoginCreatesAuthenticatedSession() throws Exception {
        MvcResult result = performValidLogin();

        MockHttpSession session =
                (MockHttpSession) result.getRequest().getSession(false);

        assertThat(session).isNotNull();

        mockMvc.perform(get("/api/admin/auth/session")
                        .session(session))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.authenticated").value(true))
                .andExpect(jsonPath("$.username").value(ADMIN_USERNAME));
    }

    @Test
    void invalidLoginIsRejected() throws Exception {
        mockMvc.perform(post("/api/admin/auth/login")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "username": "test-admin",
                                  "password": "wrong-password"
                                }
                                """))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code")
                        .value("ADMIN_AUTHENTICATION_FAILED"))
                .andExpect(jsonPath("$.message")
                        .value("Invalid administrator credentials."));
    }

    @Test
    void sessionWithAuthenticationReportsAuthenticated() throws Exception {
        MockHttpSession session = authenticatedSession();

        mockMvc.perform(get("/api/admin/auth/session")
                        .session(session))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.authenticated").value(true))
                .andExpect(jsonPath("$.username").value(ADMIN_USERNAME))
                .andExpect(jsonPath("$.csrfToken").isString())
                .andExpect(jsonPath("$.csrfHeaderName").isString());
    }

    @Test
    void sessionWithoutAuthenticationReportsUnauthenticated() throws Exception {
        mockMvc.perform(get("/api/admin/auth/session"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.authenticated").value(false))
                .andExpect(jsonPath("$.username").doesNotExist())
                .andExpect(jsonPath("$.csrfToken").isString())
                .andExpect(jsonPath("$.csrfHeaderName").isString());
    }

    @Test
    void logoutInvalidatesAuthenticatedSession() throws Exception {
        MockHttpSession session = authenticatedSession();

        mockMvc.perform(post("/api/admin/auth/logout")
                        .session(session)
                        .with(csrf()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.authenticated").value(false))
                .andExpect(jsonPath("$.status").value("LOGGED_OUT"));

        assertThat(session.isInvalid()).isTrue();

        mockMvc.perform(get("/api/admin/auth/session"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.authenticated").value(false));
    }

    @Test
    void loginResponseDoesNotExposePasswordOrHash() throws Exception {
        mockMvc.perform(post("/api/admin/auth/login")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(validLoginJson()))
                .andExpect(status().isOk())
                .andExpect(content().string(not(containsString(ADMIN_PASSWORD))))
                .andExpect(jsonPath("$.password").doesNotExist())
                .andExpect(jsonPath("$.passwordHash").doesNotExist())
                .andExpect(jsonPath("$.sessionId").doesNotExist());
    }

    @Test
    void joinRemainsPublicWithoutAdminSession() throws Exception {
        when(joinApplicationService.join(any())).thenReturn(
                new JoinResult(
                        JoinOutcome.NEW_SUBSCRIPTION,
                        "temporary-management-token"
                )
        );

        mockMvc.perform(post("/api/join")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "email": "user@example.com",
                                  "firstName": "User",
                                  "countryCode": "CR",
                                  "preferences": ["GENERAL_PREPAREDNESS"]
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("REQUEST_ACCEPTED"));
    }

    @Test
    void managementRemainsAccessibleWithBearerWithoutAdminSession()
            throws Exception {
        when(subscriptionManagementService.getSubscription(MANAGEMENT_TOKEN))
                .thenReturn(
                        new SubscriptionManagementView(
                                "User",
                                "CR",
                                Set.of(SubscriberPreference.GENERAL_PREPAREDNESS)
                        )
                );

        mockMvc.perform(get("/api/subscriptions/manage")
                        .header(
                                HttpHeaders.AUTHORIZATION,
                                "Bearer " + MANAGEMENT_TOKEN
                        ))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.countryCode").value("CR"));
    }

    @Test
    void unsubscribeRemainsPublicWithBearerWithoutAdminSession()
            throws Exception {
        mockMvc.perform(post("/api/subscriptions/unsubscribe")
                        .header(
                                HttpHeaders.AUTHORIZATION,
                                "Bearer " + MANAGEMENT_TOKEN
                        ))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("UNSUBSCRIBED"));
    }

    @Test
    void adminMutationWithoutCsrfIsBlocked() throws Exception {
        mockMvc.perform(post("/api/admin/security-check")
                        .with(user(ADMIN_USERNAME).roles("ADMIN")))
                .andExpect(status().isForbidden());
    }

    @Test
    void adminMutationWithValidCsrfPassesSecurity() throws Exception {
        mockMvc.perform(post("/api/admin/security-check")
                        .with(user(ADMIN_USERNAME).roles("ADMIN"))
                        .with(csrf()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status")
                        .value("ADMIN_MUTATION_ALLOWED"));
    }

    @Test
    void managementPatchRemainsPublicWithBearerWithoutAdminSession()
            throws Exception {
        when(subscriptionManagementService.updateSubscription(
                any(),
                any()
        )).thenReturn(
                new SubscriptionManagementView(
                        "Updated",
                        "CR",
                        Set.of(SubscriberPreference.EDUCATIONAL_CONTENT)
                )
        );

        mockMvc.perform(patch("/api/subscriptions/manage")
                        .header(
                                HttpHeaders.AUTHORIZATION,
                                "Bearer " + MANAGEMENT_TOKEN
                        )
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "firstName": "Updated",
                                  "countryCode": "CR",
                                  "preferences": ["EDUCATIONAL_CONTENT"]
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.firstName").value("Updated"));
    }

    private MvcResult performValidLogin() throws Exception {
        return mockMvc.perform(post("/api/admin/auth/login")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(validLoginJson()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("AUTHENTICATED"))
                .andExpect(jsonPath("$.authenticated").value(true))
                .andReturn();
    }

    private MockHttpSession authenticatedSession() throws Exception {
        return (MockHttpSession) performValidLogin()
                .getRequest()
                .getSession(false);
    }

    private String validLoginJson() {
        return """
                {
                  "username": "test-admin",
                  "password": "test-password"
                }
                """;
    }
}
