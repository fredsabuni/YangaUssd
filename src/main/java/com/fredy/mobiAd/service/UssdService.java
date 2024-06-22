package com.fredy.mobiAd.service;

import com.fredy.mobiAd.dto.PaymentRequestDTO;
import com.fredy.mobiAd.dto.PaymentResponseDTO;
import com.fredy.mobiAd.dto.SubscriptionRequestDTO;
import com.fredy.mobiAd.dto.SubscriptionResponseDTO;
import com.fredy.mobiAd.model.Menu;
import com.fredy.mobiAd.model.MenuItem;
import com.fredy.mobiAd.model.Player;
import com.fredy.mobiAd.model.UserSessionLog;
import com.fredy.mobiAd.repository.UserSessionLogRepository;
import com.fredy.mobiAd.repository.MenuItemRepository;
import com.fredy.mobiAd.repository.MenuRepository;
import jakarta.transaction.Transactional;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;
import java.util.stream.IntStream;


@Service
public class UssdService {
    private static final Logger log = LoggerFactory.getLogger(UssdService.class);
    private static final Long DYNAMIC_MENU_ID = 4L;
    private static final Long NEXT_MENU_ID = 5L;
    private static final String FETCH_PLAYERS_INPUT = "1*1*1";
    private static final String FETCH_PLAYERS_INPUT_1 = "1*2*1";
    private static final String FETCH_GOALS_INPUT = "1*1*2";
    private static final String FETCH_GOALS_INPUT_1 = "1*2*2";

    private Map<String, List<MenuItem>> cache = new ConcurrentHashMap<>();

    @Autowired
    private MenuRepository menuRepository;

    @Autowired
    private MenuItemRepository menuItemRepository;

    @Autowired
    private UserSessionLogRepository userSessionLogRepository;

    @Autowired
    private ExternalApiService externalApiService;

    public String handleUssdRequest(String sessionId, String phoneNumber, String text) {
        String response;
        if (text == null || text.isEmpty()) {
            response =  generateMenuResponse(1L);
        } else {
            String[] inputs = text.split("\\*");
            response =  navigateMenus(sessionId, phoneNumber,inputs);
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

    private String generateDynamicMenuResponse(Long menuId, String dynamicType) {
        Menu menu = menuRepository.findById(menuId).orElseThrow(() -> new IllegalArgumentException("Invalid menu ID: " + menuId));
        List<MenuItem> menuItems = menuItemRepository.findByMenu_Id(menuId)
                .stream()
                .filter(item -> dynamicType.equals(item.getDynamicType()))
                .toList();

        StringBuilder response = new StringBuilder("CON ").append(menu.getText()).append("\n");
        for (MenuItem item : menuItems) {
            response.append(item.getText()).append("\n");
        }

        return response.toString();
    }

    private String navigateMenus(String sessionId, String phoneNumber, String[] inputs) {
        String combinedInputs = String.join("*", inputs);
        Long menuId = 1L; // Start with the main menu

        if (inputs.length == 2 && inputs[0].equals("2")) {
            return handleNews(phoneNumber, inputs);
        }else if (FETCH_PLAYERS_INPUT.equals(combinedInputs) || FETCH_PLAYERS_INPUT_1.equals(combinedInputs)) {
            fetchAndCachePlayers();
            return generateDynamicMenuResponse(DYNAMIC_MENU_ID, "PLAYER");
        }else if (FETCH_GOALS_INPUT.equals(combinedInputs) || FETCH_GOALS_INPUT_1.equals(combinedInputs)) {
            fetchAndCacheGoals();
            return generateDynamicMenuResponse(DYNAMIC_MENU_ID, "GOAL");
        }else if (inputs.length == 5) {
            return processVote(sessionId, phoneNumber, inputs);
        } else {
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
    }

    private void fetchAndCachePlayers() {
        List<MenuItem> existingPlayers = menuItemRepository.findByDynamicType("PLAYER");
        if (!existingPlayers.isEmpty()) {
            menuItemRepository.deleteByDynamicType("PLAYER");
        }
        List<Player> players = externalApiService.fetchAndCachePlayers();
        List<MenuItem> playerMenuItems = IntStream.range(0, players.size())
                .mapToObj(i -> convertToMenuItem(players.get(i), "PLAYER", i + 1))
                .collect(Collectors.toList());
        menuItemRepository.saveAll(playerMenuItems);
    }

    private void fetchAndCacheGoals() {
        List<MenuItem> existingGoals = menuItemRepository.findByDynamicType("GOAL");
        if (!existingGoals.isEmpty()) {
            menuItemRepository.deleteByDynamicType("GOAL");
        }
        List<Player> goals = externalApiService.fetchAndCacheGoals();
        List<MenuItem> goalMenuItems = IntStream.range(0, goals.size())
                .mapToObj(i -> convertToMenuItem(goals.get(i), "GOAL", i + 1))
                .collect(Collectors.toList());
        menuItemRepository.saveAll(goalMenuItems);
    }

    private MenuItem convertToMenuItem(Player player , String dynamicType, int index) {
        Menu menu = menuRepository.findById(DYNAMIC_MENU_ID)
                .orElseThrow(() -> new IllegalArgumentException("Invalid menu ID: " + DYNAMIC_MENU_ID));

        String menuItemText = index + ". " + player.getName();
        return new MenuItem(menuItemText, player.getId(), NEXT_MENU_ID, menu, dynamicType);
    }


    private void logSession(String sessionId, String phoneNumber, String userInput, String response) {
        UserSessionLog sessionLog = new UserSessionLog(sessionId, phoneNumber, userInput, response);
        userSessionLogRepository.save(sessionLog);
    }

    private Long determineCurrentMenuId(String[] inputs) {
        Long currentMenuId = 1L; // Start with the main menu ID

        for (String input : inputs) {
            List<MenuItem> menuItems = menuItemRepository.findByMenu_Id(currentMenuId);
            MenuItem selectedItem = menuItems.stream()
                    .filter(item -> item.getText().startsWith(input + "."))
                    .findFirst()
                    .orElse(null);

            if (selectedItem != null && selectedItem.getNextMenuId() != null) {
                currentMenuId = selectedItem.getNextMenuId();
            } else {
                break;
            }
        }

        return currentMenuId;
    }

    @Transactional
    private String processVote(String sessionId, String phoneNumber, String[] inputs) {
        // Retrieve the previous menu item (player selection) and current menu item (vote unit)
        int selectedVoteUnit = Integer.parseInt(inputs[inputs.length - 1]);
        int selectedPlayerIndex = Integer.parseInt(inputs[inputs.length - 2]);

        // Retrieve dynamic menu items
        List<MenuItem> dynamicMenuItems = menuItemRepository.findByDynamicType("PLAYER");
        if (dynamicMenuItems.isEmpty()) {
            dynamicMenuItems = menuItemRepository.findByDynamicType("GOAL");
        }

        if (selectedPlayerIndex <= 0 || selectedPlayerIndex > dynamicMenuItems.size()) {
            return "END Invalid selection. Please try again.";
        }

        MenuItem selectedPlayerItem = dynamicMenuItems.get(selectedPlayerIndex - 1);
        Long playerId = selectedPlayerItem.getPlayerId();

        // Determine the current menu ID based on inputs (for vote unit)
        Long menuId = determineCurrentMenuId(inputs);

        // Retrieve current menu items for vote unit
        List<MenuItem> currentMenuItems = menuItemRepository.findByMenu_Id(menuId);
        MenuItem currentItem = currentMenuItems.stream()
                .filter(item -> item.getText().startsWith(String.valueOf(selectedVoteUnit)))
                .findFirst()
                .orElse(null);

        if (currentItem == null) {
            return "END Invalid selection. Please try again.";
        }

        Long amount = currentItem.getAmount();
//        Long menuItemId = currentItem.getId(); // Get the menu item ID

        // Log the details for debugging
        log.info("Processing vote for playerId: {}, amount: {}, phoneNumber: {}", playerId, amount, phoneNumber);

        // Process the vote
        return processVote(phoneNumber, playerId, amount);
    }

    @Transactional
    private String processVote(String phoneNumber, Long playerId, Long amount) {
        try {
        // Create and send the payment request
        PaymentRequestDTO paymentRequestDTO = new PaymentRequestDTO();
        paymentRequestDTO.setContestantId(playerId);
        paymentRequestDTO.setAmount(amount);
        paymentRequestDTO.setPhoneNumber(phoneNumber.replace("+", ""));

        // Log the request details for debugging
        log.info("Sending payment request: {}", paymentRequestDTO.getPhoneNumber());

        PaymentResponseDTO paymentResponseDTO = externalApiService.vote(paymentRequestDTO);

        // Log the response details for debugging
        log.info("Received payment response: {}", paymentResponseDTO);

        if (paymentResponseDTO.isSuccess()) {
            return "END Complete the transaction to vote!";
        } else {
//            return "END Failed to record your vote. " + paymentResponseDTO.getMessage();
            return "END Failed to record your vote. ";
            }
        }   catch (Exception e) {
                log.error("Error handling vote", e);
                return "END Voting failed.";
        }
    }

    private String handleNews(String phoneNumber, String[] inputs) {
        try {
            int selectedBundleUnit = Integer.parseInt(inputs[inputs.length - 1]);

            // Determine the current menu ID based on inputs (for vote unit)
            Long menuId = determineCurrentMenuId(inputs);

            // Retrieve current menu items for bundle unit
            List<MenuItem> currentMenuItems = menuItemRepository.findByMenu_Id(menuId);
            MenuItem currentItem = currentMenuItems.stream()
                    .filter(item -> item.getText().startsWith(String.valueOf(selectedBundleUnit)))
                    .findFirst()
                    .orElse(null);

            if (currentItem == null) {
                return "END Invalid selection. Please try again.";
            }
            Long amount = currentItem.getAmount();
            // Log the details for debugging
            log.info("Processing bundle of amount: {}, phoneNumber: {}", amount, phoneNumber);

            // Create the subscription request DTO
            SubscriptionRequestDTO requestDTO = new SubscriptionRequestDTO();
            requestDTO.setPhoneNumber(phoneNumber.replace("+", "")); // Clean phone number
            requestDTO.setAmount(amount);

            SubscriptionResponseDTO subscriptionResponseDTO = externalApiService.news(requestDTO);

            if (subscriptionResponseDTO.isSuccess()) {
                return "END Complete transaction to subscribe!";
            } else {
                return "END Subscription failed. ";
            }

        } catch (Exception e) {
            log.error("Error handling news subscription", e);
            return "END Subscription failed: " + e.getMessage();
        }
    }
}