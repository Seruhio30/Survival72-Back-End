package com.seruhioCode30.survival72.controller.admin;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api/admin/security-check")
public class AdminSecurityProbeController {

    @GetMapping
    public ResponseEntity<Map<String, String>> readCheck() {
        return ResponseEntity.ok(
                Map.of("status", "ADMIN_ACCESS_GRANTED")
        );
    }

    @PostMapping
    public ResponseEntity<Map<String, String>> mutationCheck() {
        return ResponseEntity.ok(
                Map.of("status", "ADMIN_MUTATION_ALLOWED")
        );
    }
}
