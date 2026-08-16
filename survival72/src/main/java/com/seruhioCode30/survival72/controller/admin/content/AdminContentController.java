package com.seruhioCode30.survival72.controller.admin.content;

import com.seruhioCode30.survival72.controller.admin.content.dto.AdminContentCreateRequest;
import com.seruhioCode30.survival72.controller.admin.content.dto.AdminContentPageResponse;
import com.seruhioCode30.survival72.controller.admin.content.dto.AdminContentResponse;
import com.seruhioCode30.survival72.controller.admin.content.dto.AdminContentUpdateRequest;
import com.seruhioCode30.survival72.service.admin.content.AdminContentService;
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
@RequestMapping("/api/admin/content")
public class AdminContentController {

    private final AdminContentService adminContentService;

    public AdminContentController(AdminContentService adminContentService) {
        this.adminContentService = adminContentService;
    }

    @GetMapping
    public ResponseEntity<AdminContentPageResponse> findContent(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(
                    defaultValue = "" + AdminContentService.DEFAULT_PAGE_SIZE
            ) int size,
            @RequestParam(required = false) String type,
            @RequestParam(required = false) String status
    ) {
        return ResponseEntity.ok(
                adminContentService.findContent(page, size, type, status)
        );
    }

    @GetMapping("/{id}")
    public ResponseEntity<AdminContentResponse> findById(
            @PathVariable Long id
    ) {
        return ResponseEntity.ok(adminContentService.findById(id));
    }

    @PostMapping
    public ResponseEntity<AdminContentResponse> create(
            @Valid @RequestBody AdminContentCreateRequest request
    ) {
        return ResponseEntity.ok(adminContentService.create(request));
    }

    @PatchMapping("/{id}")
    public ResponseEntity<AdminContentResponse> update(
            @PathVariable Long id,
            @Valid @RequestBody AdminContentUpdateRequest request
    ) {
        return ResponseEntity.ok(adminContentService.update(id, request));
    }
}
