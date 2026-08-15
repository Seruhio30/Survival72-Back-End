package com.seruhioCode30.survival72.controller.join;

import com.seruhioCode30.survival72.controller.join.dto.JoinRequest;
import com.seruhioCode30.survival72.controller.join.dto.JoinResponse;
import com.seruhioCode30.survival72.service.join.JoinCommand;
import com.seruhioCode30.survival72.service.join.JoinService;
import jakarta.validation.Valid;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/join")
public class JoinController {

    private static final JoinResponse ACCEPTED_RESPONSE = new JoinResponse(
            "REQUEST_ACCEPTED",
            "Join request processed."
    );

    private final JoinService joinService;

    public JoinController(JoinService joinService) {
        this.joinService = joinService;
    }

    @PostMapping(
            consumes = MediaType.APPLICATION_JSON_VALUE,
            produces = MediaType.APPLICATION_JSON_VALUE
    )
    public ResponseEntity<JoinResponse> join(
            @Valid @RequestBody JoinRequest request
    ) {
        JoinCommand command = new JoinCommand(
                request.email(),
                request.firstName(),
                request.countryCode(),
                request.preferences()
        );

        joinService.join(command);

        return ResponseEntity.ok(ACCEPTED_RESPONSE);
    }
}
