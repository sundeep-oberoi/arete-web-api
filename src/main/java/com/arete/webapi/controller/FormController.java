package com.arete.webapi.controller;

import com.arete.webapi.dto.FormData;
import com.arete.webapi.dto.OfferResponse;
import com.arete.webapi.dto.RoomCostResponse;
import com.arete.webapi.dto.SaveFormResponse;
import com.arete.webapi.dto.SaveLeaveEmailRequest;
import com.arete.webapi.service.FormService;
import com.arete.webapi.service.RoomCostService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping
public class FormController {

    private static final Logger log = LoggerFactory.getLogger(FormController.class);

    private final RoomCostService roomCostService;
    private final FormService formService;

    public FormController(RoomCostService roomCostService, FormService formService) {
        this.roomCostService = roomCostService;
        this.formService = formService;
    }

    @PostMapping("/room-cost")
    public ResponseEntity<RoomCostResponse> getRoomCost(@RequestBody FormData formData) {
        log.info("POST /room-cost received");
        return ResponseEntity.ok(roomCostService.getRoomCost());
    }

    @PostMapping("/save-leave-email")
    public ResponseEntity<Void> saveLeaveEmail(@RequestBody SaveLeaveEmailRequest request) {
        log.info("POST /save-leave-email received for email: {}", request.getEmail());
        if (request.getEmail() == null || request.getEmail().isBlank()) {
            throw new IllegalArgumentException("Email is required");
        }
        if (request.getFormData() == null) {
            throw new IllegalArgumentException("Form data is required");
        }
        formService.saveLeaveEmail(request.getEmail(), request.getFormData());
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/save-form")
    public ResponseEntity<SaveFormResponse> saveForm(@RequestBody FormData formData) {
        log.info("POST /save-form received for profile: {}", formData.getProfile());
        String uuid = formService.saveForm(formData);
        return ResponseEntity.ok(new SaveFormResponse(uuid));
    }

    @GetMapping("/offer/{uuid}")
    public ResponseEntity<OfferResponse> getOffer(@PathVariable String uuid) {
        log.info("GET /offer/{} received", uuid);
        OfferResponse response = formService.getOffer(uuid);
        return ResponseEntity.ok(response);
    }
}
