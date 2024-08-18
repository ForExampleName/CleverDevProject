package com.example.testproject.scheduler;

import com.example.testproject.service.SynchronizationService;
import org.springframework.context.annotation.Profile;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.util.concurrent.TimeUnit;

@Service
@Profile("with_sync")
public class SynchronizationScheduler {

    private final SynchronizationService synchronizationService;

    public SynchronizationScheduler(SynchronizationService synchronizationService) {
        this.synchronizationService = synchronizationService;
    }

    @Scheduled(fixedRate = 20, initialDelay = 10, timeUnit = TimeUnit.SECONDS)
    public void synchronize() {
        synchronizationService.synchronize();
    }
}
