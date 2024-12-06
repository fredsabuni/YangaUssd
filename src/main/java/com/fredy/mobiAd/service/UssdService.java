package com.fredy.mobiAd.service;

import com.fredy.mobiAd.dto.PaymentRequestDTO;
import com.fredy.mobiAd.dto.PaymentResponseDTO;
import com.fredy.mobiAd.dto.SubscriptionRequestDTO;
import com.fredy.mobiAd.dto.SubscriptionResponseDTO;
import com.fredy.mobiAd.model.*;
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
    private static final Long DYNAMIC_MENU_ID = 2L;
    private static final Long NEXT_MENU_ID = 3L;
    private static final Long DYNAMIC_PLAN_MENU_ID = 3L;
    private static final Long NEXT_MENU_PLAN_ID = 4L;
    private static final String TEAM_INPUT = "1*1";
    private static final String PLAN_INPUT = "1*2";
//    private static final String FETCH_GOALS_INPUT = "1*1*2";
//    private static final String FETCH_GOALS_INPUT_1 = "1*2*2";

    private Map<String, List<MenuItem>> cache = new ConcurrentHashMap<>();

    @Autowired
    private MenuRepository menuRepository;

    @Autowired
    private MenuItemRepository menuItemRepository;

    @Autowired
    private UserSessionLogRepository userSessionLogRepository;

    @Autowired
    private ExternalApiService externalApiService;

    private static final Long DYNAMIC_CONTEST_MENU_ID = 6L; // Assuming menu ID for contests

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


        // Handle Generic News selection
        if (combinedInputs.startsWith(TEAM_INPUT)) {
            // If the input is exactly "1*1", fetch clubs (team news)
            if (combinedInputs.equals(TEAM_INPUT)) {
                fetchAndCacheClubs();
                return generateDynamicMenuResponse(DYNAMIC_MENU_ID, "CLUB");
            }

            // After selecting a team (e.g., 1*1*3), fetch and display plans
            if (inputs.length == 3 && inputs[0].equals("1")) {
                fetchAndCachePlans();
                return generateDynamicMenuResponse(DYNAMIC_PLAN_MENU_ID, "PLAN");
            }

            // Once a plan is selected, handle the news subscription (input length == 4)
            if (inputs.length == 4 && inputs[0].equals("1")) {
                return handleNews(phoneNumber, inputs, "Generic");
            }
        }


        // Handle Plan selection independently (if needed)
        if (combinedInputs.startsWith(PLAN_INPUT)) {
            // If the input is exactly "1*2", fetch plans
            if (combinedInputs.equals(PLAN_INPUT)) {
                fetchAndCachePlans();
                return generateDynamicMenuResponse(DYNAMIC_PLAN_MENU_ID, "PLAN");
            }

            // Handle the case where the input length is 3 (selection of plan)
            if (inputs.length == 3 && inputs[0].equals("1")) {
                return handleNews(phoneNumber, inputs, "Team"); // Handle plan selection and payment
            }
        }

        // Handle the case where the input length is 1 (Voting)
        if (inputs.length == 1 && inputs[0].equals("2")) {
            fetchAndCacheContests();
            return generateDynamicMenuResponse(DYNAMIC_CONTEST_MENU_ID, "CONTEST");
        }

        // Default flow control for navigating menus

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
                    return "END Asante kwa majibu yako.";
                }
            } else {
                return "END Chaguo sio sahihi. Tafadhali jaribu tena.";
            }
        }

        return generateMenuResponse(menuId);
    }

    //Fetch Contestants
    private void fetchAndCacheContests() {
        // Fetch contests from the external API
        List<Contest> contests = externalApiService.fetchContests();

        // Delete old contest menu items
        menuItemRepository.deleteByDynamicType("CONTEST");

        // Add fetched contests as new menu items
        Menu contestMenu = menuRepository.findById(DYNAMIC_CONTEST_MENU_ID)
                .orElseThrow(() -> new IllegalArgumentException("Invalid menu ID: " + DYNAMIC_CONTEST_MENU_ID));

        List<MenuItem> contestMenuItems = IntStream.range(0, contests.size())
                .mapToObj(i -> {
                    Contest contest = contests.get(i);
                    return new MenuItem(
                            (i + 1) + ". " + contest.getName(),
                            contest.getId(),
                            null, // No next menu for contests yet
                            contestMenu,
                            "CONTEST"
                    );
                }).toList();

        menuItemRepository.saveAll(contestMenuItems);
    }


//    private void fetchAndCachePlayers() {
//        List<MenuItem> existingPlayers = menuItemRepository.findByDynamicType("PLAYER");
//        if (!existingPlayers.isEmpty()) {
//            menuItemRepository.deleteByDynamicType("PLAYER");
//        }
//        List<Player> players = externalApiService.fetchAndCachePlayers();
//        List<MenuItem> playerMenuItems = IntStream.range(0, players.size())
//                .mapToObj(i -> convertToMenuItem(players.get(i), "PLAYER", i + 1))
//                .collect(Collectors.toList());
//        menuItemRepository.saveAll(playerMenuItems);
//    }


    private void fetchAndCacheClubs() {
        try {
            List<MenuItem> existingClubs = menuItemRepository.findByDynamicType("CLUB");
            if (!existingClubs.isEmpty()) {
                menuItemRepository.deleteByDynamicType("CLUB");
            }
            List<Club> clubs = externalApiService.fetchAndCacheClubs();

            List<MenuItem> clubMenuItems = IntStream.range(0, clubs.size())
                    .mapToObj(i -> convertToMenuItem(clubs.get(i), "CLUB", i + 1))
                    .collect(Collectors.toList());
            menuItemRepository.saveAll(clubMenuItems);
        }catch (Exception e){
            e.printStackTrace();
        }

    }

    private void fetchAndCachePlans() {
        try {
            List<MenuItem> existingPlans = menuItemRepository.findByDynamicType("PLAN");
            if (!existingPlans.isEmpty()) {
                menuItemRepository.deleteByDynamicType("PLAN");
            }
            List<Plan> plans = externalApiService.fetchAndCachePlans();

            List<MenuItem> planMenuItems = IntStream.range(0, plans.size())
                    .mapToObj(i -> convertToMenuItem(plans.get(i), "PLAN", i + 1))
                    .collect(Collectors.toList());
            menuItemRepository.saveAll(planMenuItems);
        }catch (Exception e){
            e.printStackTrace();
        }

    }

//    private void fetchAndCacheGoals() {
//        List<MenuItem> existingGoals = menuItemRepository.findByDynamicType("GOAL");
//        if (!existingGoals.isEmpty()) {
//            menuItemRepository.deleteByDynamicType("GOAL");
//        }
//        List<Player> goals = externalApiService.fetchAndCacheGoals();
//        List<MenuItem> goalMenuItems = IntStream.range(0, goals.size())
//                .mapToObj(i -> convertToMenuItem(goals.get(i), "GOAL", i + 1))
//                .collect(Collectors.toList());
//        menuItemRepository.saveAll(goalMenuItems);
//    }

//    private MenuItem convertToMenuItem(Player player , String dynamicType, int index) {
//        Menu menu = menuRepository.findById(DYNAMIC_MENU_ID)
//                .orElseThrow(() -> new IllegalArgumentException("Invalid menu ID: " + DYNAMIC_MENU_ID));
//
//        String menuItemText = index + ". " + player.getName();
//        return new MenuItem(menuItemText, player.getId(), NEXT_MENU_ID, menu, dynamicType);
//    }

    private MenuItem convertToMenuItem(Club club , String dynamicType, int index) {
        Menu menu = menuRepository.findById(DYNAMIC_MENU_ID)
                .orElseThrow(() -> new IllegalArgumentException("Invalid menu ID: " + DYNAMIC_MENU_ID));

        String menuItemText = index + ". " + club.getClubName();
        return new MenuItem(menuItemText, club.getId(), NEXT_MENU_ID, menu, dynamicType);
    }

    private MenuItem convertToMenuItem(Plan plan , String dynamicType, int index) {
        Menu menu = menuRepository.findById(DYNAMIC_PLAN_MENU_ID)
                .orElseThrow(() -> new IllegalArgumentException("Invalid menu ID: " + DYNAMIC_PLAN_MENU_ID));

        String menuItemText = index + ". " + plan.getName() + " - " + plan.getAmount();
        return new MenuItem(menuItemText, plan.getId(), NEXT_MENU_PLAN_ID, menu, dynamicType,plan.getAmount());
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
            return "END Chaguo sio sahihi. Tafadhali jaribu tena.";
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
            return "END Chaguo sio sahihi. Tafadhali jaribu tena.";
        }

        Long amount = currentItem.getAmount();
        // Long menuItemId = currentItem.getId(); // Get the menu item ID

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
        // return "END Failed to record your vote. " + paymentResponseDTO.getMessage();
            return "END Kura yako haijarekodi . ";
            }
        }   catch (Exception e) {
                log.error("Error handling vote", e);
                return "END Kupiga kura kumeshindikana.";
        }
    }

    private String handleNews(String phoneNumber, String[] inputs, String topicType) {
        try {
            int selectedBundleUnit = Integer.parseInt(inputs[inputs.length - 1]);
            int selectedClub = Integer.parseInt(inputs[inputs.length - 2]);

            Long previousMenuId = DYNAMIC_MENU_ID;

            List<MenuItem> previousMenuItems = menuItemRepository.findByMenu_Id(previousMenuId);
            MenuItem selectedTeamItem = previousMenuItems.stream()
                    .filter(item -> item.getText().startsWith(String.valueOf(selectedClub)))
                    .findFirst()
                    .orElse(null);

            if (selectedTeamItem == null) {
                return "END Chaguo sio sahihi. Tafadhali jaribu tena.";
            }

            Long menuItemId = selectedTeamItem.getPlayerId(); // Get the ID of the selected team

            // Determine the current menu ID based on inputs (for vote unit)
            Long menuId = DYNAMIC_PLAN_MENU_ID;

            // Retrieve current menu items for bundle unit
            List<MenuItem> currentMenuItems = menuItemRepository.findByMenu_Id(menuId);
            MenuItem currentItem = currentMenuItems.stream()
                    .filter(item -> item.getText().startsWith(String.valueOf(selectedBundleUnit)))
                    .findFirst()
                    .orElse(null);

            if (currentItem == null) {
                return "END Chaguo sio sahihi. Tafadhali jaribu tena.";
            }
            Long amount = currentItem.getAmount();
            // Log the details for debugging
            log.info("topicId: {}, Processing bundle of amount: {}, phoneNumber: {}", menuItemId, amount, phoneNumber);

            // Create the subscription request DTO
            SubscriptionRequestDTO requestDTO = new SubscriptionRequestDTO();
            if(topicType.equals("Generic")){
                requestDTO.setTopicId("0");
            }else if (topicType.equals("Team")){
                requestDTO.setTopicId(menuItemId.toString());
            }
            requestDTO.setPaymentPhone(phoneNumber.replace("+", ""));
            requestDTO.setSubscriptionPhone(phoneNumber.replace("+", ""));
            requestDTO.setAmount(amount);
            requestDTO.setChannel("USSD");

            SubscriptionResponseDTO subscriptionResponseDTO = externalApiService.news(requestDTO);

            if (subscriptionResponseDTO.getRespCode() == 2000) {
                return "END Kamilisha muamala ili kujiunga!";
            } else {
                return "END Umeshindwa kujiunga kwa sasa.";
            }

        } catch (Exception e) {
            log.error("Error handling news subscription", e);
            return "END Umeshindwa kujiunga kwa sasa.";
        }
    }
}