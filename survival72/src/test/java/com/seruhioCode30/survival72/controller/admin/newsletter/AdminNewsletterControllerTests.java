package com.seruhioCode30.survival72.controller.admin.newsletter;

import com.seruhioCode30.survival72.config.SecurityConfig;
import com.seruhioCode30.survival72.config.properties.AdminSecurityProperties;
import com.seruhioCode30.survival72.controller.admin.newsletter.dto.AdminNewsletterAudienceMemberResponse;
import com.seruhioCode30.survival72.controller.admin.newsletter.dto.AdminNewsletterAudiencePreviewResponse;
import com.seruhioCode30.survival72.controller.admin.newsletter.dto.AdminNewsletterPageResponse;
import com.seruhioCode30.survival72.controller.admin.newsletter.dto.AdminNewsletterResponse;
import com.seruhioCode30.survival72.model.NewsletterStatus;
import com.seruhioCode30.survival72.model.SubscriberPreference;
import com.seruhioCode30.survival72.service.admin.newsletter.AdminNewsletterService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(AdminNewsletterController.class)
@Import({
        SecurityConfig.class,
        AdminNewsletterExceptionHandler.class
})
@EnableConfigurationProperties(AdminSecurityProperties.class)
class AdminNewsletterControllerTests {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private AdminNewsletterService adminNewsletterService;

    @Test
    void endpointWithoutAdminSessionIsBlocked() throws Exception {
        mockMvc.perform(get("/api/admin/newsletters"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code")
                        .value("ADMIN_AUTHENTICATION_REQUIRED"));
    }

    @Test
    void authenticatedAdminCanListNewsletters() throws Exception {
        LocalDateTime now = LocalDateTime.of(2026, 8, 17, 10, 0);

        when(adminNewsletterService.findNewsletters(
                0,
                20,
                null
        )).thenReturn(
                new AdminNewsletterPageResponse(
                        List.of(new AdminNewsletterResponse(
                                1L,
                                "Emergency kit",
                                "Prepare supplies",
                                NewsletterStatus.DRAFT,
                                Set.of(SubscriberPreference.EMERGENCY_KIT),
                                now,
                                now,
                                null
                        )),
                        0,
                        20,
                        1,
                        1,
                        false
                )
        );

        mockMvc.perform(get("/api/admin/newsletters")
                        .with(user("test-admin").roles("ADMIN")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].id").value(1))
                .andExpect(jsonPath("$.content[0].status").value("DRAFT"))
                .andExpect(jsonPath("$.page").value(0))
                .andExpect(jsonPath("$.size").value(20));
    }

    @Test
    void authenticatedAdminCanReadNewsletterById() throws Exception {
        LocalDateTime now = LocalDateTime.of(2026, 8, 17, 10, 0);

        when(adminNewsletterService.findById(7L)).thenReturn(
                new AdminNewsletterResponse(
                        7L,
                        "Subject",
                        "Body",
                        NewsletterStatus.DRAFT,
                        Set.of(SubscriberPreference.GENERAL_PREPAREDNESS),
                        now,
                        now,
                        null
                )
        );

        mockMvc.perform(get("/api/admin/newsletters/7")
                        .with(user("test-admin").roles("ADMIN")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(7))
                .andExpect(jsonPath("$.subject").value("Subject"));
    }

    @Test
    void authenticatedAdminCanCreateDraftWithCsrf() throws Exception {
        LocalDateTime now = LocalDateTime.of(2026, 8, 17, 10, 0);

        when(adminNewsletterService.create(any())).thenReturn(
                new AdminNewsletterResponse(
                        1L,
                        "Emergency kit",
                        "Prepare supplies",
                        NewsletterStatus.DRAFT,
                        Set.of(SubscriberPreference.EMERGENCY_KIT),
                        now,
                        now,
                        null
                )
        );

        mockMvc.perform(post("/api/admin/newsletters")
                        .with(user("test-admin").roles("ADMIN"))
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "subject": "Emergency kit",
                                  "body": "Prepare supplies",
                                  "preferences": ["EMERGENCY_KIT"]
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("DRAFT"))
                .andExpect(jsonPath("$.sentAt").doesNotExist());
    }

    @Test
    void invalidCreateRequestReturnsControlledBadRequest() throws Exception {
        mockMvc.perform(post("/api/admin/newsletters")
                        .with(user("test-admin").roles("ADMIN"))
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "subject": "",
                                  "body": "",
                                  "preferences": []
                                }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("BAD_REQUEST"))
                .andExpect(jsonPath("$.message")
                        .value("Invalid admin newsletter request."));
    }

    @Test
    void authenticatedAdminCanPatchNewsletterWithCsrf() throws Exception {
        LocalDateTime now = LocalDateTime.of(2026, 8, 17, 10, 0);

        when(adminNewsletterService.update(eq(5L), any())).thenReturn(
                new AdminNewsletterResponse(
                        5L,
                        "Updated",
                        "Body",
                        NewsletterStatus.DRAFT,
                        Set.of(SubscriberPreference.PRACTICAL_SKILLS),
                        now,
                        now,
                        null
                )
        );

        mockMvc.perform(patch("/api/admin/newsletters/5")
                        .with(user("test-admin").roles("ADMIN"))
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "subject": "Updated",
                                  "preferences": ["PRACTICAL_SKILLS"]
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.subject").value("Updated"))
                .andExpect(jsonPath("$.status").value("DRAFT"));
    }

    @Test
    void authenticatedAdminCanMarkNewsletterReadyWithCsrf() throws Exception {
        LocalDateTime now = LocalDateTime.of(2026, 8, 17, 10, 0);

        when(adminNewsletterService.markReady(9L)).thenReturn(
                new AdminNewsletterResponse(
                        9L,
                        "Ready",
                        "Body",
                        NewsletterStatus.READY_TO_SEND,
                        Set.of(SubscriberPreference.EVENTS_AND_UPDATES),
                        now,
                        now,
                        null
                )
        );

        mockMvc.perform(post("/api/admin/newsletters/9/ready")
                        .with(user("test-admin").roles("ADMIN"))
                        .with(csrf()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("READY_TO_SEND"));
    }

    @Test
    void audiencePreviewReturnsControlledFieldsOnly() throws Exception {
        when(adminNewsletterService.previewAudience(
                3L,
                0,
                20
        )).thenReturn(
                new AdminNewsletterAudiencePreviewResponse(
                        1,
                        List.of(new AdminNewsletterAudienceMemberResponse(
                                12L,
                                "person@example.com",
                                "Person",
                                Set.of(
                                        SubscriberPreference.EMERGENCY_KIT,
                                        SubscriberPreference.PRACTICAL_SKILLS
                                )
                        )),
                        0,
                        20,
                        1,
                        false
                )
        );

        mockMvc.perform(get("/api/admin/newsletters/3/audience-preview")
                        .with(user("test-admin").roles("ADMIN")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalAudience").value(1))
                .andExpect(jsonPath("$.content[0].email")
                        .value("person@example.com"))
                .andExpect(content().string(not(
                        containsString("managementTokenHash")
                )))
                .andExpect(content().string(not(
                        containsString("managementToken")
                )))
                .andExpect(content().string(not(
                        containsString("session")
                )));
    }
}
