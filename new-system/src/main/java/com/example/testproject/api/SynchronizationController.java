package com.example.testproject.api;

import com.example.testproject.service.SynchronizationService;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/synchronization")
public class SynchronizationController {
    private final SynchronizationService synchronizationService;

    public SynchronizationController(SynchronizationService synchronizationService) {
        this.synchronizationService = synchronizationService;
    }

    @PostMapping("/force")
    public void forceSynchronization() {
        synchronizationService.synchronize();
    }
}
