package com.seruhioCode30.survival72.controller.admin.subscriber;

import com.seruhioCode30.survival72.controller.admin.subscriber.dto.AdminSubscriberPageResponse;
import com.seruhioCode30.survival72.service.admin.subscriber.AdminSubscriberService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/admin/subscribers")
public class AdminSubscriberController {

    private final AdminSubscriberService adminSubscriberService;

    public AdminSubscriberController(
            AdminSubscriberService adminSubscriberService
    ) {
        this.adminSubscriberService = adminSubscriberService;
    }

    @GetMapping
    public ResponseEntity<AdminSubscriberPageResponse> findSubscribers(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(
                    defaultValue = "" + AdminSubscriberService.DEFAULT_PAGE_SIZE
            ) int size,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String preference
    ) {
        return ResponseEntity.ok(
                adminSubscriberService.findSubscribers(
                        page,
                        size,
                        status,
                        preference
                )
        );
    }
}
