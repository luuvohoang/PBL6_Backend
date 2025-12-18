package com.safetyconstruction.backend.controller;

import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.safetyconstruction.backend.dto.request.ApiResponse;
import com.safetyconstruction.backend.dto.request.alert.AiAlertRequest;
import com.safetyconstruction.backend.service.AlertService;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/ai")
@RequiredArgsConstructor
public class AiController {
    private final AlertService alertService;

    @PostMapping("/alerts")
    public ApiResponse<String> receiveAlert(@RequestBody AiAlertRequest request) {
        alertService.processAiAlert(request);
        return ApiResponse.<String>builder().result("Success").build();
    }
}
