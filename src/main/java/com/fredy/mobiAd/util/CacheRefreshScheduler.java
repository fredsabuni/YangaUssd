package com.fredy.mobiAd.util;

import com.fredy.mobiAd.service.ExternalApiService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class CacheRefreshScheduler {
    @Autowired
    private ExternalApiService externalApiService;

    @Scheduled(fixedRate = 1800000) // 30 minutes in milliseconds
    public void refreshCache() {
        externalApiService.fetchAndCachePlayers();
        externalApiService.fetchAndCacheGoals();
    }
}
