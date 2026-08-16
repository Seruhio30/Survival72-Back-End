package com.seruhioCode30.survival72.controller.admin.subscriber;

import com.seruhioCode30.survival72.config.SecurityConfig;
import com.seruhioCode30.survival72.config.properties.AdminSecurityProperties;
import com.seruhioCode30.survival72.controller.admin.subscriber.dto.AdminSubscriberPageResponse;
import com.seruhioCode30.survival72.controller.admin.subscriber.dto.AdminSubscriberResponse;
import com.seruhioCode30.survival72.model.SubscriberPreference;
import com.seruhioCode30.survival72.model.SubscriberStatus;
import com.seruhioCode30.survival72.service.admin.subscriber.AdminSubscriberService;
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
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(AdminSubscriberController.class)
@Import({
        SecurityConfig.class,
        AdminSubscriberExceptionHandler.class
})
@EnableConfigurationProperties(AdminSecurityProperties.class)
class AdminSubscriberControllerTests {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private AdminSubscriberService adminSubscriberService;

    @Test
    void endpointWithoutAdminSessionIsBlocked() throws Exception {
        mockMvc.perform(get("/api/admin/subscribers"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code")
                        .value("ADMIN_AUTHENTICATION_REQUIRED"));
    }

    @Test
    void authenticatedAdminCanReadSubscribers() throws Exception {
        LocalDateTime subscribedAt =
                LocalDateTime.of(2026, 8, 16, 12, 0);

        AdminSubscriberResponse subscriber =
                new AdminSubscriberResponse(
                        10L,
                        "subscriber@example.com",
                        "Subscriber",
                        "CR",
                        SubscriberStatus.ACTIVE,
                        Set.of(SubscriberPreference.EMERGENCY_KIT),
                        subscribedAt,
                        subscribedAt,
                        null
                );

        when(adminSubscriberService.findSubscribers(
                0,
                20,
                null,
                null
        )).thenReturn(
                new AdminSubscriberPageResponse(
                        List.of(subscriber),
                        0,
                        20,
                        1,
                        1,
                        false
                )
        );

        mockMvc.perform(get("/api/admin/subscribers")
                        .with(user("test-admin").roles("ADMIN")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].id").value(10))
                .andExpect(jsonPath("$.content[0].email")
                        .value("subscriber@example.com"))
                .andExpect(jsonPath("$.content[0].status")
                        .value("ACTIVE"))
                .andExpect(jsonPath("$.page").value(0))
                .andExpect(jsonPath("$.size").value(20))
                .andExpect(jsonPath("$.totalElements").value(1))
                .andExpect(jsonPath("$.totalPages").value(1))
                .andExpect(jsonPath("$.hasNext").value(false))
                .andExpect(jsonPath("$.content[0].managementTokenHash")
                        .doesNotExist())
                .andExpect(jsonPath("$.content[0].managementToken")
                        .doesNotExist())
                .andExpect(jsonPath("$.content[0].topicsOfInterest")
                        .doesNotExist())
                .andExpect(content().string(not(
                        containsString("managementTokenHash")
                )));
    }

    @Test
    void forwardsPaginationAndFilters() throws Exception {
        when(adminSubscriberService.findSubscribers(
                2,
                15,
                "ACTIVE",
                "EVENTS_AND_UPDATES"
        )).thenReturn(
                new AdminSubscriberPageResponse(
                        List.of(),
                        2,
                        15,
                        0,
                        0,
                        false
                )
        );

        mockMvc.perform(get("/api/admin/subscribers")
                        .param("page", "2")
                        .param("size", "15")
                        .param("status", "ACTIVE")
                        .param("preference", "EVENTS_AND_UPDATES")
                        .with(user("test-admin").roles("ADMIN")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.page").value(2))
                .andExpect(jsonPath("$.size").value(15));
    }

    @Test
    void invalidServiceQueryReturnsControlledBadRequest() throws Exception {
        when(adminSubscriberService.findSubscribers(
                0,
                20,
                "INVALID",
                null
        )).thenThrow(new IllegalArgumentException("Invalid status."));

        mockMvc.perform(get("/api/admin/subscribers")
                        .param("status", "INVALID")
                        .with(user("test-admin").roles("ADMIN")))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("BAD_REQUEST"))
                .andExpect(jsonPath("$.message")
                        .value("Invalid admin subscriber query."));
    }

    @Test
    void nonNumericPaginationReturnsControlledBadRequest() throws Exception {
        mockMvc.perform(get("/api/admin/subscribers")
                        .param("page", "abc")
                        .with(user("test-admin").roles("ADMIN")))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("BAD_REQUEST"))
                .andExpect(jsonPath("$.message")
                        .value("Invalid admin subscriber query."));
    }

    @Test
    void extremeSizeReturnsControlledBadRequest() throws Exception {
        when(adminSubscriberService.findSubscribers(
                0,
                101,
                null,
                null
        )).thenThrow(new IllegalArgumentException("Invalid size."));

        mockMvc.perform(get("/api/admin/subscribers")
                        .param("size", "101")
                        .with(user("test-admin").roles("ADMIN")))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("BAD_REQUEST"));
    }
}
