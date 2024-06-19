package com.fredy.mobiAd.service;

import com.fredy.mobiAd.model.Menu;
import com.fredy.mobiAd.model.MenuItem;
import com.fredy.mobiAd.model.UserSessionLog;
import com.fredy.mobiAd.repository.UserSessionLogRepository;
import com.fredy.mobiAd.repository.MenuItemRepository;
import com.fredy.mobiAd.repository.MenuRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

@Service
public class UssdService {
    private static final Logger log = LoggerFactory.getLogger(UssdService.class);
    @Autowired
    private MenuRepository menuRepository;

    @Autowired
    private MenuItemRepository menuItemRepository;

    @Autowired
    private UserSessionLogRepository userSessionLogRepository;

    public String handleUssdRequest(String sessionId, String phoneNumber, String text) {
        String response;
        if (text == null || text.isEmpty()) {
            response =  generateMenuResponse(1L);
        } else {
            String[] inputs = text.split("\\*");
            response =  navigateMenus(inputs);
        }

        logSession(sessionId, phoneNumber, text, response);
        return response;
    }

    private String generateMenuResponse(Long menuId) {
        Menu menu = menuRepository.findById(menuId).orElseThrow(() -> new IllegalArgumentException("Invalid menu ID: " + menuId));
        List<MenuItem> menuItems = menuItemRepository.findByMenu_Id(menuId);

        StringBuilder response = new StringBuilder("CON ").append(menu.getText()).append("\n");
        for (MenuItem item : menuItems) {
            response.append(item.getText()).append("\n");
        }

        return response.toString();
    }

    private String navigateMenus(String[] inputs) {
        Long menuId = 1L; // Start with the main menu

        for (String input : inputs) {
            List<MenuItem> menuItems = menuItemRepository.findByMenu_Id(menuId);
            MenuItem menuItem = menuItems.stream()
                    .filter(item -> item.getText().startsWith(input))
                    .findFirst()
                    .orElse(null);

            if (menuItem != null) {
                if (menuItem.getNextMenuId() != null) {
                    menuId = menuItem.getNextMenuId();
                } else {
                    return "END Thank you for your response.";
                }
            } else {
                return "END Invalid choice. Please try again.";
            }
        }

        return generateMenuResponse(menuId);
    }

    private void logSession(String sessionId, String phoneNumber, String userInput, String response) {
        UserSessionLog sessionLog = new UserSessionLog(sessionId, phoneNumber, userInput, response);
        userSessionLogRepository.save(sessionLog);
    }
}
