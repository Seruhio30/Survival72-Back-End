package com.seruhioCode30.survival72.controller.subscription;

import com.seruhioCode30.survival72.controller.subscription.dto.SubscriptionManagementRequest;
import com.seruhioCode30.survival72.controller.subscription.dto.SubscriptionManagementResponse;
import com.seruhioCode30.survival72.model.SubscriberPreference;
import com.seruhioCode30.survival72.service.subscription.SubscriptionAccessException;
import com.seruhioCode30.survival72.service.subscription.SubscriptionManagementService;
import com.seruhioCode30.survival72.service.subscription.SubscriptionManagementView;
import com.seruhioCode30.survival72.service.subscription.UpdateSubscriptionCommand;
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
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@AutoConfigureMockMvc(addFilters = false)
@WebMvcTest(SubscriptionManagementController.class)
class SubscriptionManagementControllerTests {

    private static final String VALID_TOKEN = "valid-management-token";

    private static final SubscriptionManagementView CURRENT_VIEW =
            new SubscriptionManagementView(
                    "Sergio",
                    "CR",
                    Set.of(
                            SubscriberPreference.GENERAL_PREPAREDNESS,
                            SubscriberPreference.EMERGENCY_KIT
                    )
            );

    private static final String VALID_PATCH_REQUEST = """
            {
              "firstName": "Sergio",
              "countryCode": "CR",
              "preferences": [
                "GENERAL_PREPAREDNESS",
                "EDUCATIONAL_CONTENT"
              ]
            }
            """;

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private SubscriptionManagementService subscriptionManagementService;

    @Test
    void validBearerTokenReturnsSubscription() throws Exception {
        when(subscriptionManagementService.getSubscription(VALID_TOKEN))
                .thenReturn(CURRENT_VIEW);

        mockMvc.perform(get("/api/subscriptions/manage")
                        .header(
                                HttpHeaders.AUTHORIZATION,
                                "Bearer " + VALID_TOKEN
                        ))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(
                        MediaType.APPLICATION_JSON
                ))
                .andExpect(jsonPath("$.firstName").value("Sergio"))
                .andExpect(jsonPath("$.countryCode").value("CR"))
                .andExpect(jsonPath("$.preferences").isArray());

        verify(subscriptionManagementService)
                .getSubscription(VALID_TOKEN);
    }

    @Test
    void getResponseContainsOnlyAllowedPublicFields() throws Exception {
        when(subscriptionManagementService.getSubscription(VALID_TOKEN))
                .thenReturn(CURRENT_VIEW);

        mockMvc.perform(get("/api/subscriptions/manage")
                        .header(
                                HttpHeaders.AUTHORIZATION,
                                "Bearer " + VALID_TOKEN
                        ))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.firstName").value("Sergio"))
                .andExpect(jsonPath("$.countryCode").value("CR"))
                .andExpect(jsonPath("$.preferences").isArray())
                .andExpect(jsonPath("$.id").doesNotExist())
                .andExpect(jsonPath("$.email").doesNotExist())
                .andExpect(jsonPath("$.status").doesNotExist())
                .andExpect(jsonPath("$.subscribedAt").doesNotExist())
                .andExpect(jsonPath("$.updatedAt").doesNotExist())
                .andExpect(jsonPath("$.unsubscribedAt").doesNotExist())
                .andExpect(jsonPath("$.managementToken").doesNotExist())
                .andExpect(jsonPath("$.managementTokenHash").doesNotExist())
                .andExpect(jsonPath("$.token").doesNotExist());
    }

    @Test
    void missingAuthorizationReturnsControlledNotFound() throws Exception {
        mockMvc.perform(get("/api/subscriptions/manage"))
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
        mockMvc.perform(get("/api/subscriptions/manage")
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
        mockMvc.perform(get("/api/subscriptions/manage")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer "))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code")
                        .value("SUBSCRIPTION_ACCESS_NOT_FOUND"));
    }

    @Test
    void invalidTokenReturnsNeutralNotFound() throws Exception {
        when(subscriptionManagementService
                .getSubscription("invalid-token"))
                .thenThrow(new SubscriptionAccessException());

        mockMvc.perform(get("/api/subscriptions/manage")
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
        when(subscriptionManagementService
                .getSubscription("revoked-token"))
                .thenThrow(new SubscriptionAccessException());

        mockMvc.perform(get("/api/subscriptions/manage")
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
    void managementResponseDtoDoesNotExposeInternalFields() {
        Set<String> componentNames = Arrays.stream(
                        SubscriptionManagementResponse.class
                                .getRecordComponents()
                )
                .map(RecordComponent::getName)
                .collect(Collectors.toSet());

        assertThat(componentNames)
                .containsExactlyInAnyOrder(
                        "firstName",
                        "countryCode",
                        "preferences"
                )
                .doesNotContain(
                        "id",
                        "email",
                        "status",
                        "subscribedAt",
                        "updatedAt",
                        "unsubscribedAt",
                        "managementToken",
                        "managementTokenHash",
                        "token"
                );
    }

    @Test
    void validPatchReturnsUpdatedSubscription() throws Exception {
        SubscriptionManagementView updatedView =
                new SubscriptionManagementView(
                        "Updated",
                        "US",
                        Set.of(SubscriberPreference.EDUCATIONAL_CONTENT)
                );

        when(subscriptionManagementService.updateSubscription(
                eq(VALID_TOKEN),
                any(UpdateSubscriptionCommand.class)
        )).thenReturn(updatedView);

        mockMvc.perform(patch("/api/subscriptions/manage")
                        .header(
                                HttpHeaders.AUTHORIZATION,
                                "Bearer " + VALID_TOKEN
                        )
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(VALID_PATCH_REQUEST))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.firstName").value("Updated"))
                .andExpect(jsonPath("$.countryCode").value("US"))
                .andExpect(jsonPath("$.preferences[0]")
                        .value("EDUCATIONAL_CONTENT"));
    }

    @Test
    void validPatchMapsRequestToUpdateSubscriptionCommand()
            throws Exception {
        when(subscriptionManagementService.updateSubscription(
                anyString(),
                any(UpdateSubscriptionCommand.class)
        )).thenReturn(CURRENT_VIEW);

        mockMvc.perform(patch("/api/subscriptions/manage")
                        .header(
                                HttpHeaders.AUTHORIZATION,
                                "Bearer " + VALID_TOKEN
                        )
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(VALID_PATCH_REQUEST))
                .andExpect(status().isOk());

        var captor = org.mockito.ArgumentCaptor.forClass(
                UpdateSubscriptionCommand.class
        );

        verify(subscriptionManagementService)
                .updateSubscription(eq(VALID_TOKEN), captor.capture());

        UpdateSubscriptionCommand command = captor.getValue();

        assertThat(command.firstName()).isEqualTo("Sergio");
        assertThat(command.countryCode()).isEqualTo("CR");
        assertThat(command.preferences())
                .containsExactlyInAnyOrder(
                        SubscriberPreference.GENERAL_PREPAREDNESS,
                        SubscriberPreference.EDUCATIONAL_CONTENT
                );
    }

    @Test
    void patchResponseContainsUpdatedPublicFieldsOnly() throws Exception {
        when(subscriptionManagementService.updateSubscription(
                anyString(),
                any(UpdateSubscriptionCommand.class)
        )).thenReturn(CURRENT_VIEW);

        mockMvc.perform(patch("/api/subscriptions/manage")
                        .header(
                                HttpHeaders.AUTHORIZATION,
                                "Bearer " + VALID_TOKEN
                        )
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(VALID_PATCH_REQUEST))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.firstName").value("Sergio"))
                .andExpect(jsonPath("$.countryCode").value("CR"))
                .andExpect(jsonPath("$.preferences").isArray())
                .andExpect(jsonPath("$.id").doesNotExist())
                .andExpect(jsonPath("$.email").doesNotExist())
                .andExpect(jsonPath("$.status").doesNotExist())
                .andExpect(jsonPath("$.managementToken").doesNotExist())
                .andExpect(jsonPath("$.managementTokenHash").doesNotExist())
                .andExpect(jsonPath("$.token").doesNotExist());
    }

    @Test
    void firstNameIsOptionalForPatch() throws Exception {
        when(subscriptionManagementService.updateSubscription(
                anyString(),
                any(UpdateSubscriptionCommand.class)
        )).thenReturn(
                new SubscriptionManagementView(
                        null,
                        "CR",
                        Set.of(SubscriberPreference.GENERAL_PREPAREDNESS)
                )
        );

        mockMvc.perform(patch("/api/subscriptions/manage")
                        .header(
                                HttpHeaders.AUTHORIZATION,
                                "Bearer " + VALID_TOKEN
                        )
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "countryCode": "CR",
                                  "preferences": [
                                    "GENERAL_PREPAREDNESS"
                                  ]
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.firstName").doesNotExist());

        var captor = org.mockito.ArgumentCaptor.forClass(
                UpdateSubscriptionCommand.class
        );

        verify(subscriptionManagementService)
                .updateSubscription(eq(VALID_TOKEN), captor.capture());

        assertThat(captor.getValue().firstName()).isNull();
    }

    @Test
    void invalidCountryCodeReturnsBadRequest() throws Exception {
        mockMvc.perform(patch("/api/subscriptions/manage")
                        .header(
                                HttpHeaders.AUTHORIZATION,
                                "Bearer " + VALID_TOKEN
                        )
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "firstName": "Sergio",
                                  "countryCode": "CRI",
                                  "preferences": [
                                    "GENERAL_PREPAREDNESS"
                                  ]
                                }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("BAD_REQUEST"))
                .andExpect(jsonPath("$.message")
                        .value(
                                "Invalid subscription management request."
                        ));
    }

    @Test
    void emptyPreferencesReturnBadRequest() throws Exception {
        mockMvc.perform(patch("/api/subscriptions/manage")
                        .header(
                                HttpHeaders.AUTHORIZATION,
                                "Bearer " + VALID_TOKEN
                        )
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "firstName": "Sergio",
                                  "countryCode": "CR",
                                  "preferences": []
                                }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("BAD_REQUEST"));
    }

    @Test
    void invalidPreferenceReturnsControlledBadRequest() throws Exception {
        mockMvc.perform(patch("/api/subscriptions/manage")
                        .header(
                                HttpHeaders.AUTHORIZATION,
                                "Bearer " + VALID_TOKEN
                        )
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "firstName": "Sergio",
                                  "countryCode": "CR",
                                  "preferences": [
                                    "UNKNOWN_PREFERENCE"
                                  ]
                                }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("BAD_REQUEST"))
                .andExpect(jsonPath("$.message")
                        .value(
                                "Invalid subscription management request."
                        ));
    }

    @Test
    void invalidTokenForPatchReturnsNeutralNotFound() throws Exception {
        when(subscriptionManagementService.updateSubscription(
                eq("invalid-token"),
                any(UpdateSubscriptionCommand.class)
        )).thenThrow(new SubscriptionAccessException());

        mockMvc.perform(patch("/api/subscriptions/manage")
                        .header(
                                HttpHeaders.AUTHORIZATION,
                                "Bearer invalid-token"
                        )
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(VALID_PATCH_REQUEST))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code")
                        .value("SUBSCRIPTION_ACCESS_NOT_FOUND"))
                .andExpect(jsonPath("$.message")
                        .value(
                                "The subscription management link is invalid or no longer available."
                        ));
    }

    @Test
    void patchDoesNotExposeOrRotateManagementToken() throws Exception {
        when(subscriptionManagementService.updateSubscription(
                eq(VALID_TOKEN),
                any(UpdateSubscriptionCommand.class)
        )).thenReturn(CURRENT_VIEW);

        mockMvc.perform(patch("/api/subscriptions/manage")
                        .header(
                                HttpHeaders.AUTHORIZATION,
                                "Bearer " + VALID_TOKEN
                        )
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(VALID_PATCH_REQUEST))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.managementToken").doesNotExist())
                .andExpect(jsonPath("$.managementTokenHash").doesNotExist())
                .andExpect(jsonPath("$.token").doesNotExist());

        verify(subscriptionManagementService)
                .updateSubscription(
                        eq(VALID_TOKEN),
                        any(UpdateSubscriptionCommand.class)
                );
    }

    @Test
    void patchRequestDtoContainsOnlyEditableFields() {
        Set<String> componentNames = Arrays.stream(
                        SubscriptionManagementRequest.class
                                .getRecordComponents()
                )
                .map(RecordComponent::getName)
                .collect(Collectors.toSet());

        assertThat(componentNames)
                .containsExactlyInAnyOrder(
                        "firstName",
                        "countryCode",
                        "preferences"
                )
                .doesNotContain(
                        "id",
                        "email",
                        "status",
                        "subscribedAt",
                        "updatedAt",
                        "unsubscribedAt",
                        "managementToken",
                        "managementTokenHash",
                        "token"
                );
    }
}
