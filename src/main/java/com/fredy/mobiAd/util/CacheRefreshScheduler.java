package com.fredy.mobiAd.util;

import com.fredy.mobiAd.service.ExternalApiService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.io.IOException;

@Component
public class CacheRefreshScheduler {
    private static final Logger log = LoggerFactory.getLogger(CacheRefreshScheduler.class);

    @Autowired
    private ExternalApiService externalApiService;

    @Scheduled(fixedRate = 6 * 60 * 60 * 1000) // 12 hours in milliseconds
    @CacheEvict(value = {"clubs", "plans", "partners", "contests"}, allEntries = true)
    public void refreshCache() throws IOException {
        log.info("Refreshing all caches...");

        externalApiService.fetchAndCachePartners();
        externalApiService.fetchContests();

        log.info("All caches (clubs, plans, partners, contests) evicted and refreshed");

    }
}
