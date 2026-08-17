package com.seruhioCode30.survival72.controller.admin.newsletter;

import com.seruhioCode30.survival72.controller.admin.newsletter.dto.AdminNewsletterAudiencePreviewResponse;
import com.seruhioCode30.survival72.controller.admin.newsletter.dto.AdminNewsletterCreateRequest;
import com.seruhioCode30.survival72.controller.admin.newsletter.dto.AdminNewsletterPageResponse;
import com.seruhioCode30.survival72.controller.admin.newsletter.dto.AdminNewsletterResponse;
import com.seruhioCode30.survival72.controller.admin.newsletter.dto.AdminNewsletterUpdateRequest;
import com.seruhioCode30.survival72.service.admin.newsletter.AdminNewsletterService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/admin/newsletters")
public class AdminNewsletterController {

    private final AdminNewsletterService adminNewsletterService;

    public AdminNewsletterController(
            AdminNewsletterService adminNewsletterService
    ) {
        this.adminNewsletterService = adminNewsletterService;
    }

    @GetMapping
    public ResponseEntity<AdminNewsletterPageResponse> findNewsletters(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(
                    defaultValue = "" + AdminNewsletterService.DEFAULT_PAGE_SIZE
            ) int size,
            @RequestParam(required = false) String status
    ) {
        return ResponseEntity.ok(
                adminNewsletterService.findNewsletters(page, size, status)
        );
    }

    @GetMapping("/{id}")
    public ResponseEntity<AdminNewsletterResponse> findById(
            @PathVariable Long id
    ) {
        return ResponseEntity.ok(adminNewsletterService.findById(id));
    }

    @PostMapping
    public ResponseEntity<AdminNewsletterResponse> create(
            @Valid @RequestBody AdminNewsletterCreateRequest request
    ) {
        return ResponseEntity.ok(adminNewsletterService.create(request));
    }

    @PatchMapping("/{id}")
    public ResponseEntity<AdminNewsletterResponse> update(
            @PathVariable Long id,
            @Valid @RequestBody AdminNewsletterUpdateRequest request
    ) {
        return ResponseEntity.ok(
                adminNewsletterService.update(id, request)
        );
    }

    @GetMapping("/{id}/audience-preview")
    public ResponseEntity<AdminNewsletterAudiencePreviewResponse>
    previewAudience(
            @PathVariable Long id,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(
                    defaultValue = "" + AdminNewsletterService.DEFAULT_PAGE_SIZE
            ) int size
    ) {
        return ResponseEntity.ok(
                adminNewsletterService.previewAudience(id, page, size)
        );
    }

    @PostMapping("/{id}/ready")
    public ResponseEntity<AdminNewsletterResponse> markReady(
            @PathVariable Long id
    ) {
        return ResponseEntity.ok(
                adminNewsletterService.markReady(id)
        );
    }
}
