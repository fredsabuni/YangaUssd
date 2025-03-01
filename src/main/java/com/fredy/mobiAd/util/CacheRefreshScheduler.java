package com.fredy.mobiAd.util;

import com.fredy.mobiAd.service.ExternalApiService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class CacheRefreshScheduler {
    private static final Logger log = LoggerFactory.getLogger(CacheRefreshScheduler.class);

    @Autowired
    private ExternalApiService externalApiService;

    @Scheduled(fixedRate = 12 * 60 * 60 * 1000) // 12 hours in milliseconds
    @CacheEvict(value = "clubs", allEntries = true)
    public void refreshCache() {
        externalApiService.fetchAndCachePlayers();
        externalApiService.fetchAndCacheGoals();
    }

    @CacheEvict(value = "plans", allEntries = true)
    public void evictPlansCache() {
        log.info("Plans cache evicted");
    }
}
