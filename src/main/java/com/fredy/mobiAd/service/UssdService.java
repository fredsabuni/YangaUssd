package com.fredy.mobiAd.service;

import com.fredy.mobiAd.dto.*;
import com.fredy.mobiAd.model.*;
import com.fredy.mobiAd.repository.ContestantRepository;
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
    private static final Long PARTNER_MENU_ID = 2L;
    private static final Long CHOOSE_NEWS_MENU_ID = 3L;
    private static final Long NEXT_MENU_ID = 3L;
    private static final Long DYNAMIC_PLAN_MENU_ID = 5L;
    private static final Long NEXT_MENU_PLAN_ID = 4L;
    private static final Long CHOOSE_VOTE_MENU_ID = 6L;
    private static final Long VOTE_PLAN_MENU_ID = 7L;
    private static final String TEAM_INPUT = "1*1*1";
    private static final String GENERIC_NEWS_INPUT = "1*1*2";

    private static final String FIRST_GOAL_INPUT = "2*1*1";
    private static final String FIRST_WIN_INPUT = "2*1*2";
//    private static final String FETCH_GOALS_INPUT_1 = "1*2*2";


    private Map<String, List<MenuItem>> cache = new ConcurrentHashMap<>();

    @Autowired
    private MenuRepository menuRepository;

    @Autowired
    private MenuItemRepository menuItemRepository;

    @Autowired
    private ContestantRepository contestantRepository;

    @Autowired
    private UserSessionLogRepository userSessionLogRepository;

    @Autowired
    private ExternalApiService externalApiService;

    private static final Long DYNAMIC_CONTEST_MENU_ID = 6L; //  menu ID for contests
    private static final Long DYNAMIC_CONTESTANT_MENU_ID = 7L; //  menu ID for contestants

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

        StringBuilder response = new StringBuilder("").append(menu.getText()).append("\n");
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

        StringBuilder response = new StringBuilder("").append(menu.getText()).append("\n");
        for (MenuItem item : menuItems) {
            response.append(item.getText()).append("\n");
        }

        return response.toString();
    }


    private String navigateMenus(String sessionId, String phoneNumber, String[] inputs) {
        String combinedInputs = String.join("*", inputs);
        Long menuId = 1L; // Start with the main menu

        // Determine the current menu ID based on inputs (for vote unit)
        menuId = determineCurrentMenuId(inputs);

        // Handle the "1" path (News-related options)
        if (inputs.length >= 1 && inputs[0].equals("1")) {
            if (inputs.length == 1) {
                // Input: "1" - Show partner menu
                return generateMenuResponse(PARTNER_MENU_ID);
            }

            if (inputs.length == 2) {
                // Input: "1*1" or "1*2" - Show news type selection
                return generateMenuResponse(CHOOSE_NEWS_MENU_ID);
            }

            // Handle Team News Path: "1*1*1"
            if (combinedInputs.startsWith(TEAM_INPUT)) {
                if (combinedInputs.equals(TEAM_INPUT)) {
                    // Input: "1*1*1" - Fetch clubs and show team selection
                    fetchAndCacheClubs();
                    return generateDynamicMenuResponse(DYNAMIC_MENU_ID, "CLUB");
                }
                if (inputs.length == 4) {
                    // Input: "1*1*1*X" - Fetch plans after team selection
                    fetchAndCachePlans();
                    return generateDynamicMenuResponse(DYNAMIC_PLAN_MENU_ID, "PLAN");
                }
                if (inputs.length == 5) {
                    // Input: "1*1*1*X*Y" - Handle team news subscription
                    return handleNews(phoneNumber, inputs, "Team");
                }
            }

            // Handle Generic News Path: "1*1*2"
            if (combinedInputs.startsWith(GENERIC_NEWS_INPUT)) {
                if (combinedInputs.equals(GENERIC_NEWS_INPUT)) {
                    // Input: "1*1*2" - Fetch plans for generic news
                    fetchAndCachePlans();
                    return generateDynamicMenuResponse(DYNAMIC_PLAN_MENU_ID, "PLAN");
                }
                if (inputs.length == 4) {
                    // Input: "1*1*2*X" - Handle generic news subscription
                    return handleNews(phoneNumber, inputs, "Generic");
                }
            }
        }

        // Handle the "2" path (Vote-related options)
        if (inputs.length >= 1 && inputs[0].equals("2")) {
            if (inputs.length == 1) {
                // Input: "1" - Show partner menu
                return generateMenuResponse(PARTNER_MENU_ID);
            }

            if (inputs.length == 2) {
                // Input: "2*1" or "2*2" or "2*3" - Show news type selection
                return generateMenuResponse(CHOOSE_VOTE_MENU_ID);
            }

            if (inputs.length == 3 && (combinedInputs.equals(FIRST_GOAL_INPUT) || combinedInputs.equals(FIRST_WIN_INPUT))) {
                // Input: "2*2*1" or "2*2*2" - Show news type selection
                return generateMenuResponse(VOTE_PLAN_MENU_ID);
            }

        }

        // Handle the case where the input length is 1 (Voting)
//        if (inputs.length == 1 && inputs[0].equals("2")) {
//            fetchAndCacheContests();
//            return generateDynamicMenuResponse(DYNAMIC_CONTEST_MENU_ID, "CONTEST");
//        }

        // Handle contests and contestants
//        if (inputs.length == 2 && inputs[0].equals("2")) {
//            Long contestId = determineContestId(inputs[1]);
//            fetchAndCacheContestants(contestId);
//            return generateDynamicMenuResponse(DYNAMIC_CONTESTANT_MENU_ID, "CONTESTANT");
//        }

        //Trigger Voting
//        if (inputs.length == 4 && inputs[0].equals("2")) {
//            return handleVote(phoneNumber, inputs);
//        }



        // Handle Plan selection For Voting
//        if(inputs.length == 3 && (inputs[0] + "*" + inputs[1]).equals("2*1")){
//            fetchAndCachePlans();
//            return generateDynamicMenuResponse(DYNAMIC_PLAN_MENU_ID, "PLAN");
//        } else if (inputs.length == 3 && (inputs[0] + "*" + inputs[1]).equals("2*2")) {
//            fetchAndCachePlans();
//            return generateDynamicMenuResponse(DYNAMIC_PLAN_MENU_ID, "PLAN");
//        }


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
                    return "Asante kwa Majibu Yako.";
                }
            } else {
                return "Chaguo sio Sahihi. Tafadhali Jaribu Tena.";
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

    private void fetchAndCacheContestants(Long contestId) {
        // Fetch contestants from the external API
        List<Contestant> contestants = externalApiService.fetchContestants(contestId);

        // Delete old contestant menu items
        menuItemRepository.deleteByDynamicType("CONTESTANT");

        // Add fetched contestants as new menu items
        Menu contestantMenu = menuRepository.findById(DYNAMIC_CONTESTANT_MENU_ID)
                .orElseThrow(() -> new IllegalArgumentException("Invalid menu ID: " + DYNAMIC_CONTESTANT_MENU_ID));

        List<MenuItem> contestantMenuItems = IntStream.range(0, contestants.size())
                .mapToObj(i -> {
                    Contestant contestant = contestants.get(i);
                    return new MenuItem(
                            (i + 1) + ". " + contestant.getName() + " - " + contestant.getClub(),
                            contestant.getId(),
                            null, // No next menu for contestants yet
                            contestantMenu,
                            "CONTESTANT"
                    );
                }).toList();

        menuItemRepository.saveAll(contestantMenuItems);
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
            List<Club> clubs = externalApiService.fetchAndCacheClubs();
            if (clubs == null || clubs.isEmpty()) {
                log.warn("No clubs available to cache");
                return;
            }

            List<MenuItem> existingClubs = menuItemRepository.findByDynamicType("CLUB");

            //Check for Mismatch between existingClub Fetched and one locally
            if (existingClubs.isEmpty() || existingClubs.size() != clubs.size()) {
                log.info("CLUB MenuItems missing or count mismatch (existing: {}, new: {}), updating...",
                        existingClubs.size(), clubs.size());
                if (!existingClubs.isEmpty()) {
                    menuItemRepository.deleteByDynamicType("CLUB");
                }
                List<MenuItem> clubMenuItems = IntStream.range(0, clubs.size())
                        .mapToObj(i -> convertToMenuItem(clubs.get(i), "CLUB", i + 1))
                        .collect(Collectors.toList());
                menuItemRepository.saveAll(clubMenuItems);
                log.info("Cached {} clubs as MenuItems", clubMenuItems.size());
            } else {
                log.info("CLUB MenuItems already exist and match count ({}), skipping update", existingClubs.size());
            }
        } catch (Exception e) {
            log.error("Error fetching and caching clubs", e);
        }
    }

    private void fetchAndCachePlans() {
        try {
            List<Plan> plans = externalApiService.fetchAndCachePlans();
            if (plans == null || plans.isEmpty()) {
                log.warn("No plans available to cache");
                return;
            }

            List<MenuItem> existingPlans = menuItemRepository.findByDynamicType("PLAN");

            // Only delete and insert if there are no existing plans
            if (existingPlans.isEmpty()) {
                log.info("No existing PLAN MenuItems found, creating new ones");
                List<MenuItem> planMenuItems = IntStream.range(0, plans.size())
                        .mapToObj(i -> convertToMenuItem(plans.get(i), "PLAN", i + 1))
                        .collect(Collectors.toList());
                menuItemRepository.saveAll(planMenuItems);
                log.info("Cached {} plans as MenuItems", planMenuItems.size());
            } else {
                log.info("PLAN MenuItems already exist, skipping update ({} plans found)", existingPlans.size());
                // Optional: Add a change detection mechanism here (see below)
            }
        } catch (Exception e) {
            log.error("Error fetching and caching plans", e);
        }

    }

    @Transactional
    private String handleVote(String phoneNumber, String[] inputs) {
        try {
            // The second last input represents the contestant selection
            int selectedContestantIndex = Integer.parseInt(inputs[inputs.length - 2]);
            // The last input represents the plan selection
            int selectedPlanIndex = Integer.parseInt(inputs[inputs.length - 1]);

            // Retrieve contestant details from the dynamic menu
            List<MenuItem> contestantMenuItems = menuItemRepository.findByDynamicType("CONTESTANT");
            if (contestantMenuItems.isEmpty()) {
                return "Hakuna Washiriki Waliopo.";
            }

            // Validate contestant index
            if (selectedContestantIndex <= 0 || selectedContestantIndex > contestantMenuItems.size()) {
                return "Uteuzi wa Mshiriki si Sahihi. Tafadhali Jaribu Tena.";
            }

            MenuItem selectedContestantMenuItem = contestantMenuItems.get(selectedContestantIndex - 1);
            Long contestantId = selectedContestantMenuItem.getPlayerId(); // Assuming playerId represents the contestant ID

            // Fetch the contestant details from the Contestant table
            Contestant contestant = contestantRepository.findById(contestantId)
                    .orElseThrow(() -> new IllegalArgumentException("Invalid contestant ID: " + contestantId));
            String votingCode = contestant.getVotingCode(); // Fetch the voting code

            // Determine the current menu ID based on inputs (for vote unit)
            Long menuId = DYNAMIC_PLAN_MENU_ID;

            // Retrieve current menu items for bundle unit
            List<MenuItem> currentMenuItems = menuItemRepository.findByMenu_Id(menuId);
            MenuItem currentItem = currentMenuItems.stream()
                    .filter(item -> item.getText().startsWith(String.valueOf(selectedPlanIndex)))
                    .findFirst()
                    .orElse(null);

            if (currentItem == null) {
                return "Chaguo sio sahihi. Tafadhali Jaribu Tena.";
            }
            Long amount = currentItem.getAmount();
            // Log the details for debugging
            log.info("votingCode: {}, Processing voting of amount: {}, phoneNumber: {}", votingCode, amount, phoneNumber);

            // Prepare the vote request
            VoteRequestDTO voteRequest = new VoteRequestDTO();
            voteRequest.setContestantCode(votingCode); // Use the voting code from the Contestant table
            voteRequest.setPhoneNumber(phoneNumber.replace("+", "")); // Clean the phone number
            voteRequest.setChannel("USSD"); // Set the voting channel
            voteRequest.setAmount(amount);

            // Call the external API to submit the vote
            VoteResponseDTO voteResponse = externalApiService.submitVote(voteRequest);

            if (voteResponse != null && voteResponse.getRespCode() == 2000) {
                return "Kura yako Imefanikiwa Kuwasilishwa!";
            } else {
                return "Kura Yako imeshindwa Kuwasilishwa. Tafadhali jaribu tena baadaye.";
            }

        } catch (Exception e) {
            log.error("Error handling vote: ", e);
            return "Kura yako imeshindwa kuwasilishwa. Tafadhali jaribu tena baadaye.";
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

    private Long determineContestId(String input) {
        List<MenuItem> contestMenuItems = menuItemRepository.findByDynamicType("CONTEST");
        MenuItem selectedContest = contestMenuItems.stream()
                .filter(item -> item.getText().startsWith(input + "."))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Invalid contest selection."));
        return selectedContest.getPlayerId(); // Assuming `playerId` stores the contestId
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
            return "Kamilisha muamala ili kupiga Kura!";
        } else {
        // return "END Failed to record your vote. " + paymentResponseDTO.getMessage();
            return "Kura yako haijarekodi . ";
            }
        }   catch (Exception e) {
                log.error("Error handling vote", e);
                return "Kupiga kura kumeshindikana.";
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
                return "Kamilisha muamala ili Kujiunga!";
            } else {
                return "Umeshindwa kujiunga kwa Sasa.";
            }

        } catch (Exception e) {
            log.error("Error handling news subscription", e);
            return "Umeshindwa kujiunga kwa Sasa.";
        }
    }




}