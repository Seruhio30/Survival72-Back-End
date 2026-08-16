package com.seruhioCode30.survival72.controller.admin.content;

import com.seruhioCode30.survival72.config.SecurityConfig;
import com.seruhioCode30.survival72.config.properties.AdminSecurityProperties;
import com.seruhioCode30.survival72.controller.admin.content.dto.AdminContentPageResponse;
import com.seruhioCode30.survival72.controller.admin.content.dto.AdminContentResponse;
import com.seruhioCode30.survival72.model.ContentStatus;
import com.seruhioCode30.survival72.model.ContentType;
import com.seruhioCode30.survival72.model.SubscriberPreference;
import com.seruhioCode30.survival72.service.admin.content.AdminContentService;
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

@WebMvcTest(AdminContentController.class)
@Import({
        SecurityConfig.class,
        AdminContentExceptionHandler.class
})
@EnableConfigurationProperties(AdminSecurityProperties.class)
class AdminContentControllerTests {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private AdminContentService adminContentService;

    @Test
    void endpointWithoutAdminSessionIsBlocked() throws Exception {
        mockMvc.perform(get("/api/admin/content"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code")
                        .value("ADMIN_AUTHENTICATION_REQUIRED"));
    }

    @Test
    void authenticatedAdminCanListContent() throws Exception {
        LocalDateTime now = LocalDateTime.of(2026, 8, 16, 15, 0);

        AdminContentResponse contentItem = new AdminContentResponse(
                10L,
                ContentType.ARTICLE,
                ContentStatus.DRAFT,
                "Emergency plan",
                "Family preparedness article",
                null,
                Set.of(SubscriberPreference.GENERAL_PREPAREDNESS),
                null,
                now,
                now
        );

        when(adminContentService.findContent(
                0,
                20,
                null,
                null
        )).thenReturn(
                new AdminContentPageResponse(
                        List.of(contentItem),
                        0,
                        20,
                        1,
                        1,
                        false
                )
        );

        mockMvc.perform(get("/api/admin/content")
                        .with(user("test-admin").roles("ADMIN")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].id").value(10))
                .andExpect(jsonPath("$.content[0].type").value("ARTICLE"))
                .andExpect(jsonPath("$.content[0].status").value("DRAFT"))
                .andExpect(jsonPath("$.content[0].title")
                        .value("Emergency plan"))
                .andExpect(jsonPath("$.page").value(0))
                .andExpect(jsonPath("$.size").value(20))
                .andExpect(jsonPath("$.totalElements").value(1))
                .andExpect(jsonPath("$.hasNext").value(false))
                .andExpect(content().string(not(
                        containsString("contentItemRepository")
                )));
    }

    @Test
    void forwardsPaginationAndFilters() throws Exception {
        when(adminContentService.findContent(
                2,
                15,
                "VIDEO",
                "PUBLISHED"
        )).thenReturn(
                new AdminContentPageResponse(
                        List.of(),
                        2,
                        15,
                        0,
                        0,
                        false
                )
        );

        mockMvc.perform(get("/api/admin/content")
                        .param("page", "2")
                        .param("size", "15")
                        .param("type", "VIDEO")
                        .param("status", "PUBLISHED")
                        .with(user("test-admin").roles("ADMIN")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.page").value(2))
                .andExpect(jsonPath("$.size").value(15));
    }

    @Test
    void authenticatedAdminCanReadContentById() throws Exception {
        LocalDateTime now = LocalDateTime.of(2026, 8, 16, 15, 0);

        when(adminContentService.findById(7L)).thenReturn(
                new AdminContentResponse(
                        7L,
                        ContentType.VIDEO,
                        ContentStatus.PUBLISHED,
                        "Water storage",
                        null,
                        "dQw4w9WgXcQ",
                        Set.of(),
                        now,
                        now,
                        now
                )
        );

        mockMvc.perform(get("/api/admin/content/7")
                        .with(user("test-admin").roles("ADMIN")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(7))
                .andExpect(jsonPath("$.youtubeVideoId")
                        .value("dQw4w9WgXcQ"));
    }

    @Test
    void authenticatedAdminCanCreateArticleWithCsrf() throws Exception {
        LocalDateTime now = LocalDateTime.of(2026, 8, 16, 15, 0);

        when(adminContentService.create(any())).thenReturn(
                new AdminContentResponse(
                        1L,
                        ContentType.ARTICLE,
                        ContentStatus.DRAFT,
                        "Emergency kit",
                        "Article",
                        null,
                        Set.of(SubscriberPreference.EMERGENCY_KIT),
                        null,
                        now,
                        now
                )
        );

        mockMvc.perform(post("/api/admin/content")
                        .with(user("test-admin").roles("ADMIN"))
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "type": "ARTICLE",
                                  "title": "Emergency kit",
                                  "description": "Article",
                                  "preferences": ["EMERGENCY_KIT"]
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.type").value("ARTICLE"))
                .andExpect(jsonPath("$.youtubeVideoId").doesNotExist());
    }

    @Test
    void adminCreateWithoutCsrfIsBlocked() throws Exception {
        mockMvc.perform(post("/api/admin/content")
                        .with(user("test-admin").roles("ADMIN"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "type": "ARTICLE",
                                  "title": "Emergency kit"
                                }
                                """))
                .andExpect(status().isForbidden());
    }

    @Test
    void authenticatedAdminCanPatchContentWithCsrf() throws Exception {
        LocalDateTime now = LocalDateTime.of(2026, 8, 16, 15, 0);

        when(adminContentService.update(eq(5L), any())).thenReturn(
                new AdminContentResponse(
                        5L,
                        ContentType.ARTICLE,
                        ContentStatus.ARCHIVED,
                        "Updated title",
                        null,
                        null,
                        Set.of(),
                        null,
                        now,
                        now
                )
        );

        mockMvc.perform(patch("/api/admin/content/5")
                        .with(user("test-admin").roles("ADMIN"))
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "title": "Updated title",
                                  "status": "ARCHIVED"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("ARCHIVED"))
                .andExpect(jsonPath("$.title").value("Updated title"));
    }

    @Test
    void invalidRequestReturnsControlledBadRequest() throws Exception {
        mockMvc.perform(post("/api/admin/content")
                        .with(user("test-admin").roles("ADMIN"))
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "type": "ARTICLE",
                                  "title": ""
                                }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("BAD_REQUEST"))
                .andExpect(jsonPath("$.message")
                        .value("Invalid admin content request."));
    }
}
