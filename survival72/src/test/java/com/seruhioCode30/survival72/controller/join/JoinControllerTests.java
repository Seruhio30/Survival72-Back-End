package com.seruhioCode30.survival72.controller.join;

import com.seruhioCode30.survival72.model.SubscriberPreference;
import com.seruhioCode30.survival72.service.join.JoinCommand;
import com.seruhioCode30.survival72.service.join.JoinOutcome;
import com.seruhioCode30.survival72.service.join.JoinResult;
import com.seruhioCode30.survival72.service.join.JoinApplicationService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@AutoConfigureMockMvc(addFilters = false)
@WebMvcTest(JoinController.class)
class JoinControllerTests {

    private static final String VALID_REQUEST = """
            {
              "email": "sergio@example.com",
              "firstName": "Sergio",
              "countryCode": "CR",
              "preferences": ["GENERAL_PREPAREDNESS"]
            }
            """;

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private JoinApplicationService joinApplicationService;

    @Test
    void validRequestReturnsNeutralAcceptedResponse() throws Exception {
        when(joinApplicationService.join(any())).thenReturn(
                new JoinResult(JoinOutcome.NEW_SUBSCRIPTION, "raw-token")
        );

        mockMvc.perform(post("/api/join")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(VALID_REQUEST))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.status").value("REQUEST_ACCEPTED"))
                .andExpect(jsonPath("$.message").value("Join request processed."));

        verify(joinApplicationService).join(any());
    }

    @Test
    void validRequestMapsPublicDtoToJoinCommand() throws Exception {
        when(joinApplicationService.join(any())).thenReturn(
                new JoinResult(JoinOutcome.NEW_SUBSCRIPTION, "raw-token")
        );

        mockMvc.perform(post("/api/join")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(VALID_REQUEST))
                .andExpect(status().isOk());

        var captor = org.mockito.ArgumentCaptor.forClass(JoinCommand.class);
        verify(joinApplicationService).join(captor.capture());

        JoinCommand command = captor.getValue();

        assertThat(command.email()).isEqualTo("sergio@example.com");
        assertThat(command.firstName()).isEqualTo("Sergio");
        assertThat(command.countryCode()).isEqualTo("CR");
        assertThat(command.preferences())
                .containsExactly(SubscriberPreference.GENERAL_PREPAREDNESS);
    }

    @Test
    void newAndActiveDuplicateReturnSamePublicResponse() throws Exception {
        assertNeutralResponse(
                new JoinResult(JoinOutcome.NEW_SUBSCRIPTION, "new-token")
        );

        assertNeutralResponse(
                new JoinResult(JoinOutcome.ACTIVE_DUPLICATE, null)
        );
    }

    @Test
    void rejoinReturnsSameNeutralPublicResponse() throws Exception {
        assertNeutralResponse(
                new JoinResult(JoinOutcome.REJOINED, "rejoin-token")
        );
    }

    @Test
    void invalidEmailReturnsBadRequest() throws Exception {
        mockMvc.perform(post("/api/join")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "email": "not-an-email",
                                  "firstName": "Sergio",
                                  "countryCode": "CR",
                                  "preferences": ["GENERAL_PREPAREDNESS"]
                                }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value("BAD_REQUEST"))
                .andExpect(jsonPath("$.message").value("Invalid join request."));
    }

    @Test
    void missingEmailReturnsBadRequest() throws Exception {
        mockMvc.perform(post("/api/join")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "countryCode": "CR",
                                  "preferences": ["GENERAL_PREPAREDNESS"]
                                }
                                """))
                .andExpect(status().isBadRequest());
    }

    @Test
    void invalidCountryCodeReturnsBadRequest() throws Exception {
        mockMvc.perform(post("/api/join")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "email": "sergio@example.com",
                                  "countryCode": "CRI",
                                  "preferences": ["GENERAL_PREPAREDNESS"]
                                }
                                """))
                .andExpect(status().isBadRequest());
    }

    @Test
    void emptyPreferencesReturnBadRequest() throws Exception {
        mockMvc.perform(post("/api/join")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "email": "sergio@example.com",
                                  "countryCode": "CR",
                                  "preferences": []
                                }
                                """))
                .andExpect(status().isBadRequest());
    }

    @Test
    void practicalSkillsPreferenceIsAccepted() throws Exception {
        when(joinApplicationService.join(any())).thenReturn(
                new JoinResult(JoinOutcome.NEW_SUBSCRIPTION, "raw-token")
        );

        mockMvc.perform(post("/api/join")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "email": "sergio@example.com",
                                  "countryCode": "CR",
                                  "preferences": ["PRACTICAL_SKILLS"]
                                }
                                """))
                .andExpect(status().isOk());

        var captor = org.mockito.ArgumentCaptor.forClass(JoinCommand.class);
        verify(joinApplicationService).join(captor.capture());

        assertThat(captor.getValue().preferences())
                .containsExactly(SubscriberPreference.PRACTICAL_SKILLS);
    }

    @Test
    void eventsAndUpdatesPreferenceIsAccepted() throws Exception {
        when(joinApplicationService.join(any())).thenReturn(
                new JoinResult(JoinOutcome.NEW_SUBSCRIPTION, "raw-token")
        );

        mockMvc.perform(post("/api/join")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "email": "sergio@example.com",
                                  "countryCode": "CR",
                                  "preferences": ["EVENTS_AND_UPDATES"]
                                }
                                """))
                .andExpect(status().isOk());

        var captor = org.mockito.ArgumentCaptor.forClass(JoinCommand.class);
        verify(joinApplicationService).join(captor.capture());

        assertThat(captor.getValue().preferences())
                .containsExactly(SubscriberPreference.EVENTS_AND_UPDATES);
    }

    @Test
    void legacyPreferenceValuesReturnControlledBadRequest() throws Exception {
        for (String legacyPreference : new String[]{
                "EDUCATIONAL_CONTENT",
                "EVENTS_AND_TRAINING"
        }) {
            mockMvc.perform(post("/api/join")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("""
                                    {
                                      "email": "sergio@example.com",
                                      "countryCode": "CR",
                                      "preferences": ["%s"]
                                    }
                                    """.formatted(legacyPreference)))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.status").value("BAD_REQUEST"))
                    .andExpect(jsonPath("$.message")
                            .value("Invalid join request."));
        }
    }

    @Test
    void invalidPreferenceReturnsControlledBadRequest() throws Exception {
        mockMvc.perform(post("/api/join")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "email": "sergio@example.com",
                                  "countryCode": "CR",
                                  "preferences": ["UNKNOWN_PREFERENCE"]
                                }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value("BAD_REQUEST"))
                .andExpect(jsonPath("$.message").value("Invalid join request."));
    }

    @Test
    void serviceContractRejectionReturnsControlledBadRequest() throws Exception {
        when(joinApplicationService.join(any())).thenThrow(
                new IllegalArgumentException("internal validation detail")
        );

        mockMvc.perform(post("/api/join")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(VALID_REQUEST))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value("BAD_REQUEST"))
                .andExpect(jsonPath("$.message").value("Invalid join request."));
    }

    @Test
    void firstNameIsOptional() throws Exception {
        when(joinApplicationService.join(any())).thenReturn(
                new JoinResult(JoinOutcome.NEW_SUBSCRIPTION, "raw-token")
        );

        mockMvc.perform(post("/api/join")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "email": "sergio@example.com",
                                  "countryCode": "CR",
                                  "preferences": ["GENERAL_PREPAREDNESS"]
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("REQUEST_ACCEPTED"));
    }

    @Test
    void responseDoesNotExposeInternalJoinData() throws Exception {
        when(joinApplicationService.join(any())).thenReturn(
                new JoinResult(JoinOutcome.REJOINED, "super-secret-raw-token")
        );

        mockMvc.perform(post("/api/join")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(VALID_REQUEST))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.rawManagementToken").doesNotExist())
                .andExpect(jsonPath("$.managementToken").doesNotExist())
                .andExpect(jsonPath("$.managementTokenHash").doesNotExist())
                .andExpect(jsonPath("$.outcome").doesNotExist())
                .andExpect(jsonPath("$.id").doesNotExist())
                .andExpect(jsonPath("$.email").doesNotExist())
                .andExpect(jsonPath("$.firstName").doesNotExist())
                .andExpect(jsonPath("$.countryCode").doesNotExist())
                .andExpect(jsonPath("$.preferences").doesNotExist());
    }

    private void assertNeutralResponse(JoinResult result) throws Exception {
        when(joinApplicationService.join(any())).thenReturn(result);

        mockMvc.perform(post("/api/join")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(VALID_REQUEST))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("REQUEST_ACCEPTED"))
                .andExpect(jsonPath("$.message").value("Join request processed."))
                .andExpect(jsonPath("$.outcome").doesNotExist())
                .andExpect(jsonPath("$.rawManagementToken").doesNotExist());
    }
}
