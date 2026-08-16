package com.seruhioCode30.survival72.controller.subscription;

import com.seruhioCode30.survival72.controller.subscription.dto.SubscriptionUnsubscribeResponse;
import com.seruhioCode30.survival72.service.subscription.SubscriptionAccessException;
import com.seruhioCode30.survival72.service.subscription.SubscriptionUnsubscribeApplicationService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.lang.reflect.RecordComponent;
import java.util.Arrays;
import java.util.Set;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@AutoConfigureMockMvc(addFilters = false)
@WebMvcTest(SubscriptionUnsubscribeController.class)
class SubscriptionUnsubscribeControllerTests {

    private static final String VALID_TOKEN = "valid-management-token";

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private SubscriptionUnsubscribeApplicationService unsubscribeApplicationService;

    @Test
    void validBearerTokenReturnsUnsubscribed() throws Exception {
        mockMvc.perform(post("/api/subscriptions/unsubscribe")
                        .header(
                                HttpHeaders.AUTHORIZATION,
                                "Bearer " + VALID_TOKEN
                        ))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(
                        MediaType.APPLICATION_JSON
                ))
                .andExpect(jsonPath("$.status").value("UNSUBSCRIBED"))
                .andExpect(jsonPath("$.message")
                        .value("Subscription cancelled successfully."));
    }

    @Test
    void unsubscribeServiceReceivesRawBearerToken() throws Exception {
        mockMvc.perform(post("/api/subscriptions/unsubscribe")
                        .header(
                                HttpHeaders.AUTHORIZATION,
                                "Bearer " + VALID_TOKEN
                        ))
                .andExpect(status().isOk());

        verify(unsubscribeApplicationService)
                .unsubscribe(VALID_TOKEN);
    }

    @Test
    void successResponseContainsOnlyPublicFields() throws Exception {
        mockMvc.perform(post("/api/subscriptions/unsubscribe")
                        .header(
                                HttpHeaders.AUTHORIZATION,
                                "Bearer " + VALID_TOKEN
                        ))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("UNSUBSCRIBED"))
                .andExpect(jsonPath("$.message")
                        .value("Subscription cancelled successfully."))
                .andExpect(jsonPath("$.id").doesNotExist())
                .andExpect(jsonPath("$.email").doesNotExist())
                .andExpect(jsonPath("$.subscribedAt").doesNotExist())
                .andExpect(jsonPath("$.updatedAt").doesNotExist())
                .andExpect(jsonPath("$.unsubscribedAt").doesNotExist())
                .andExpect(jsonPath("$.preferences").doesNotExist())
                .andExpect(jsonPath("$.managementToken").doesNotExist())
                .andExpect(jsonPath("$.managementTokenHash").doesNotExist())
                .andExpect(jsonPath("$.token").doesNotExist());
    }

    @Test
    void missingAuthorizationReturnsControlledNotFound() throws Exception {
        mockMvc.perform(post("/api/subscriptions/unsubscribe"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code")
                        .value("SUBSCRIPTION_ACCESS_NOT_FOUND"))
                .andExpect(jsonPath("$.message")
                        .value(
                                "The subscription management link is invalid or no longer available."
                        ));
    }

    @Test
    void nonBearerAuthorizationReturnsControlledNotFound() throws Exception {
        mockMvc.perform(post("/api/subscriptions/unsubscribe")
                        .header(
                                HttpHeaders.AUTHORIZATION,
                                "Basic " + VALID_TOKEN
                        ))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code")
                        .value("SUBSCRIPTION_ACCESS_NOT_FOUND"));
    }

    @Test
    void emptyBearerTokenReturnsControlledNotFound() throws Exception {
        mockMvc.perform(post("/api/subscriptions/unsubscribe")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer "))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code")
                        .value("SUBSCRIPTION_ACCESS_NOT_FOUND"));
    }

    @Test
    void invalidTokenReturnsNeutralNotFound() throws Exception {
        doThrow(new SubscriptionAccessException())
                .when(unsubscribeApplicationService)
                .unsubscribe("invalid-token");

        mockMvc.perform(post("/api/subscriptions/unsubscribe")
                        .header(
                                HttpHeaders.AUTHORIZATION,
                                "Bearer invalid-token"
                        ))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code")
                        .value("SUBSCRIPTION_ACCESS_NOT_FOUND"))
                .andExpect(jsonPath("$.message")
                        .value(
                                "The subscription management link is invalid or no longer available."
                        ));
    }

    @Test
    void revokedTokenReturnsSameNeutralNotFound() throws Exception {
        doThrow(new SubscriptionAccessException())
                .when(unsubscribeApplicationService)
                .unsubscribe("revoked-token");

        mockMvc.perform(post("/api/subscriptions/unsubscribe")
                        .header(
                                HttpHeaders.AUTHORIZATION,
                                "Bearer revoked-token"
                        ))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code")
                        .value("SUBSCRIPTION_ACCESS_NOT_FOUND"))
                .andExpect(jsonPath("$.message")
                        .value(
                                "The subscription management link is invalid or no longer available."
                        ));
    }

    @Test
    void nonActiveSubscriberReturnsSameNeutralNotFound() throws Exception {
        doThrow(new SubscriptionAccessException())
                .when(unsubscribeApplicationService)
                .unsubscribe("non-active-token");

        mockMvc.perform(post("/api/subscriptions/unsubscribe")
                        .header(
                                HttpHeaders.AUTHORIZATION,
                                "Bearer non-active-token"
                        ))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code")
                        .value("SUBSCRIPTION_ACCESS_NOT_FOUND"))
                .andExpect(jsonPath("$.message")
                        .value(
                                "The subscription management link is invalid or no longer available."
                        ));
    }

    @Test
    void responseDtoDoesNotExposeInternalFields() {
        Set<String> componentNames = Arrays.stream(
                        SubscriptionUnsubscribeResponse.class
                                .getRecordComponents()
                )
                .map(RecordComponent::getName)
                .collect(Collectors.toSet());

        assertThat(componentNames)
                .containsExactlyInAnyOrder(
                        "status",
                        "message"
                )
                .doesNotContain(
                        "id",
                        "email",
                        "subscribedAt",
                        "updatedAt",
                        "unsubscribedAt",
                        "preferences",
                        "managementToken",
                        "managementTokenHash",
                        "token"
                );
    }

    @Test
    void emailQueryParameterDoesNotAuthorizeUnsubscribe() throws Exception {
        mockMvc.perform(post("/api/subscriptions/unsubscribe")
                        .queryParam("email", "user@example.com"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code")
                        .value("SUBSCRIPTION_ACCESS_NOT_FOUND"));

        verify(unsubscribeApplicationService, never())
                .unsubscribe("user@example.com");
    }

    @Test
    void tokenQueryParameterDoesNotAuthorizeUnsubscribe() throws Exception {
        mockMvc.perform(post("/api/subscriptions/unsubscribe")
                        .queryParam("token", VALID_TOKEN))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code")
                        .value("SUBSCRIPTION_ACCESS_NOT_FOUND"));

        verify(unsubscribeApplicationService, never())
                .unsubscribe(VALID_TOKEN);
    }

    @Test
    void getDoesNotExecuteUnsubscribe() throws Exception {
        mockMvc.perform(get("/api/subscriptions/unsubscribe")
                        .header(
                                HttpHeaders.AUTHORIZATION,
                                "Bearer " + VALID_TOKEN
                        ))
                .andExpect(status().isMethodNotAllowed());

        verify(unsubscribeApplicationService, never())
                .unsubscribe(VALID_TOKEN);
    }
}
