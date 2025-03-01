package com.fredy.mobiAd.controller;

import com.fredy.mobiAd.service.UssdService;
import jakarta.annotation.PreDestroy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

@RestController
public class UssdController {
    private static final Logger log = LoggerFactory.getLogger(UssdController.class);

    @Autowired
    private UssdService ussdService;

    // Map to store the USSD path for each session
//    private Map<String, String> ussdPathMap = new ConcurrentHashMap<>();

    private final Map<String, SessionData> sessionDataMap = new ConcurrentHashMap<>(1000, 0.75f, 16);
    private final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);
    private static final long SESSION_TIMEOUT = 4 * 60 * 1000;

    private static class SessionData {
        String ussdPath;
        long lastActivityTime;

        SessionData(String ussdPath, long lastActivityTime) {
            this.ussdPath = ussdPath;
            this.lastActivityTime = lastActivityTime;
        }
    }

    public UssdController() {
        // Schedule a task to clean up stale sessions every minute
        scheduler.scheduleAtFixedRate(this::cleanupStaleSessions, 1, 1, TimeUnit.MINUTES);
    }


    @RequestMapping(value = "/", method = {RequestMethod.GET, RequestMethod.POST})
    public String ussdCallback(@RequestParam Map<String, String> requestParams) {
        String sessionId = requestParams.get("sessionId");
        String phoneNumber = requestParams.get("phoneNumber");
        String text = requestParams.get("text");


        // Handle the default menu case where text is empty
//        if (text == null || text.isEmpty()) {
//            ussdPathMap.put(sessionId, "");
//        } else {
//            // Corrected path generation logic
//            String currentPath = ussdPathMap.getOrDefault(sessionId, "");
//            String newPath;
//            if (currentPath.isEmpty()) {
//                newPath = text; // For the first response, directly use the text
//            } else {
//                newPath = currentPath + "*" + text; // Append with asterisk for subsequent responses
//            }
//            ussdPathMap.put(sessionId, newPath);
//        }
//
//        // Use the full path for processing
//        String fullPath = ussdPathMap.get(sessionId);
//        log.info("{FullPath}"+ fullPath);

        if (sessionId == null || phoneNumber == null) {
            log.warn("Invalid request parameters: sessionId={}, phoneNumber={}", sessionId, phoneNumber);
            return "Vigezo vya ombi si sahihi.";
        }

        // Get or create session data
        SessionData sessionData = sessionDataMap.compute(sessionId, (key, existingData) -> {
            if (existingData == null) {
                // New session
                return new SessionData("", System.currentTimeMillis());
            } else {
                // Update last activity time
                existingData.lastActivityTime = System.currentTimeMillis();
                return existingData;
            }
        });

        // Handle the default menu case where text is empty
        String currentPath = sessionData.ussdPath;
        String newPath = text == null || text.isEmpty() ? "" : currentPath.isEmpty() ? text : currentPath + "*" + text;
        sessionData.ussdPath = newPath;

        // Use the full path for processing
        log.info("FullPath: {}", newPath);

        return ussdService.handleUssdRequest(sessionId,phoneNumber,newPath);
    }


    private void cleanupStaleSessions() {
        long currentTime = System.currentTimeMillis();
        sessionDataMap.entrySet().removeIf(entry -> {
            SessionData sessionData = entry.getValue();
            boolean isStale = (currentTime - sessionData.lastActivityTime) > SESSION_TIMEOUT;
            if (isStale) {
                log.info("Removed stale session: sessionId={}", entry.getKey());
            }
            return isStale;
        });
    }

    @PreDestroy
    public void shutdown() {
        scheduler.shutdown();
    }
}
