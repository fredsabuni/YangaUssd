package com.fredy.mobiAd.service;

import com.fredy.mobiAd.dto.*;
import com.fredy.mobiAd.model.*;
import com.fredy.mobiAd.repository.ContestantRepository;
import com.fredy.mobiAd.repository.UserSessionLogRepository;
import com.fredy.mobiAd.repository.MenuItemRepository;
import com.fredy.mobiAd.repository.MenuRepository;
import com.fredy.mobiAd.util.MenuItemParser;
import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import jakarta.transaction.Transactional;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
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
    private static final Long DYNAMIC_CONTEST_MENU_ID = 6L; //  menu ID for contests
    private static final Long DYNAMIC_CONTESTANT_MENU_ID = 7L;
    private static final Long LOCAL_PACKAGES_MENU_ID = 8L;

    //  menu ID for contestants
    private static final String HABARI_INPUT = "1*1";
    private static final String SPORT_NEWS_INPUT = "1*1*1";
    private static final String GENERIC_NEWS_INPUT = "1*1*2";
    private static final String VOTE_INPUT = "1*2";


    private static final String FIRST_GOAL_INPUT = "2*1*1";
    private static final String FIRST_WIN_INPUT = "2*1*2";


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

    // Cache for menus, partners, clubs, and plans
    private final Cache<String, List<MenuItem>> cache = Caffeine.newBuilder()
            .expireAfterWrite(5, java.util.concurrent.TimeUnit.HOURS)
            .build();


    public  ResponseEntity<String> handleUssdRequest(String sessionId, String phoneNumber, String text, String player) {
        ResponseEntity<String> response;
        if (text == null || text.isEmpty()) {
            fetchAndCacheParterns();
            response =  buildResponse(generateDynamicMenuResponse(PARTNER_MENU_ID, "PARTNER"), player, false);
        } else {
            String[] inputs = text.split("\\*");
            response =  navigateMenus(sessionId, phoneNumber,inputs, player);
        }

        logSession(sessionId, phoneNumber, text, response.getBody());
        return response;
    }

    private String generateMenuResponse(Long menuId) {
        Menu menu = menuRepository.findById(menuId).orElseThrow(() -> new IllegalArgumentException("Invalid menu ID: " + menuId));
        List<MenuItem> menuItems = menuItemRepository.findByMenu_Id(menuId);

        StringBuilder response = new StringBuilder(menu.getText()).append("\n");
        for (MenuItem item : menuItems) {
            response.append(item.getText()).append("\n");
        }

        return response.toString();
    }

    private String generateDynamicMenuResponse(Long menuId, String dynamicType) {
        Menu menu = menuRepository.findById(menuId).orElseThrow(() -> new IllegalArgumentException("Invalid menu ID: " + menuId));

        List<MenuItem> menuItems = cache.get("menuItems_" + menuId + "_" + dynamicType, k -> menuItemRepository.findByMenu_Id(menuId)
                .stream()
                .filter(item -> dynamicType.equals(item.getDynamicType()))
                .toList());

        StringBuilder response = new StringBuilder(menu.getText()).append("\n");
        for (MenuItem item : menuItems) {
            response.append(item.getText()).append("\n");
        }

        return response.toString();
    }


    public ResponseEntity<String> navigateMenus(String sessionId, String phoneNumber, String[] inputs, String player) {
        String combinedInputs = String.join("*", inputs);
        Long menuId = 1L;

        // Determine the current menu ID based on inputs (for vote unit)
        menuId = determineCurrentMenuId(inputs);

        if (inputs.length >= 1 && inputs[0].equals("1")) {

            if (inputs.length == 1) {
                return buildResponse(generateMenuResponse(menuId), player, false);
            }

            // Handle News Path: "1*1"
            if (combinedInputs.startsWith(HABARI_INPUT)) {
                switch (inputs.length) {
                    case 2: // "1*1"
                        return buildResponse(generateMenuResponse(CHOOSE_NEWS_MENU_ID), player, false);

                    case 3: // "1*1*1" or "1*1*2"
                        if (combinedInputs.equals(SPORT_NEWS_INPUT) ||
                                combinedInputs.equals(GENERIC_NEWS_INPUT)) {
                            return buildResponse(generateDynamicMenuResponse(DYNAMIC_PLAN_MENU_ID, "PLAN"), player, false);
                        }
                        break;

                    case 4: // "1*1*1*X"
                        return buildResponse(handleNews(phoneNumber, inputs, "Team"), player, true);
                }
            }

            // Handle Vote Path: "1*2"
            if (combinedInputs.startsWith(VOTE_INPUT)) {
                switch (inputs.length) {
                    case 2: // "1*2"
                        fetchAndCacheContests();
                        return buildResponse(generateDynamicMenuResponse(CHOOSE_VOTE_MENU_ID, "CONTEST"), player, false);

                    case 3: // "1*2*X"
                        Long contestId = determineContestId(inputs[2]);
                        fetchAndCacheContestants(contestId);
                        return buildResponse(generateDynamicMenuResponse(VOTE_PLAN_MENU_ID, "CONTESTANT"), player, false);

                    case 4: // "1*2*X*X"
                        return buildResponse(generateMenuResponse(LOCAL_PACKAGES_MENU_ID), player, false);

                    case 5: // "1*2*X*X*X"
                        return buildResponse(handleVote(phoneNumber, inputs), player, true);
                }
            }
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
                    return buildResponse("Asante kwa Majibu Yako.", player, true);
                }
            } else {
                return buildResponse("Chaguo sio Sahihi. Tafadhali Jaribu Tena.", player, true);
            }
        }

        return buildResponse(generateMenuResponse(menuId), player, false);
    }

    private ResponseEntity<String> buildResponse(String responseText, String player, boolean isFinal) {
        HttpHeaders headers = new HttpHeaders();
        headers.add("Freeflow", isFinal ? "FB" : "FC");
        return ResponseEntity.ok().headers(headers).body(responseText);
    }


    //Fetch Contestants
    private void fetchAndCacheContests() {
        try {
            // Fetch contest MenuItems from the cache, if not found, fetch contests from API, convert to MenuItems, and cache them
            List<MenuItem> contestMenuItems = cache.get("contests", k -> {
                List<Contest> contests = externalApiService.fetchContests();
                if (contests == null || contests.isEmpty()) {
                    log.warn("No contests available to cache");
                    return List.of(); // Return an empty list if no contests are found
                }
                return IntStream.range(0, contests.size())
                        .mapToObj(i -> convertToMenuItem(contests.get(i), "CONTEST", i + 1))
                        .collect(Collectors.toList());
            });

            // Retrieve existing contest MenuItems from the database
            List<MenuItem> existingContests = menuItemRepository.findByDynamicType("CONTEST");

            // Check if the number of cached contest MenuItems matches the number in the database
            if (existingContests.size() != contestMenuItems.size()) {
                log.info("CONTEST MenuItems missing or count mismatch (existing: {}, new: {}), updating...", existingContests.size(), contestMenuItems.size());

                // Delete existing contest MenuItems from the database if they are outdated
                if (!existingContests.isEmpty()) {
                    menuItemRepository.deleteByDynamicType("CONTEST");
                }

                // Save the new contest MenuItems to the database
                menuItemRepository.saveAll(contestMenuItems);
                log.info("Cached {} contests as MenuItems", contestMenuItems.size());
            } else {
                log.info("CONTEST MenuItems already exist and match count ({}), skipping update", existingContests.size());
            }
        } catch (Exception e) {
            log.error("Error fetching and caching contests", e);
        }
    }

    private void fetchAndCacheContestants(Long contestId) {
        try{
            // Fetch contestant MenuItems from the cache, if not found, fetch contestants from API, convert to MenuItems, and cache them
            List<MenuItem> contestantMenuItems = cache.get("contestants_" + contestId, k -> {
                List<Contestant> contestants = externalApiService.fetchContestants(contestId);
                if (contestants == null || contestants.isEmpty()) {
                    log.warn("No contestants available to cache for contestId: {}", contestId);
                    return List.of(); // Return an empty list if no contestants are found
                }
                return IntStream.range(0, contestants.size())
                        .mapToObj(i -> convertToMenuItem(contestants.get(i), "CONTESTANT", i + 1))
                        .collect(Collectors.toList());
            });

            // Retrieve existing contestant MenuItems from the database
            List<MenuItem> existingContestants = menuItemRepository.findByDynamicType("CONTESTANT");

            // Check if the number of cached contestant MenuItems matches the number in the database
            if (existingContestants.size() != contestantMenuItems.size()) {
                log.info("CONTESTANT MenuItems missing or count mismatch (existing: {}, new: {}) for contestId: {}, updating...",
                        existingContestants.size(), contestantMenuItems.size(), contestId);

                // Delete existing contestant MenuItems from the database if they are outdated
                if (!existingContestants.isEmpty()) {
                    menuItemRepository.deleteByDynamicType("CONTESTANT");
                }

                // Save the new contestant MenuItems to the database
                menuItemRepository.saveAll(contestantMenuItems);
                log.info("Cached {} contestants as MenuItems for contestId: {}", contestantMenuItems.size(), contestId);
            } else {
                log.info("CONTESTANT MenuItems already exist and match count ({}) for contestId: {}, skipping update",
                        existingContestants.size(), contestId);
            }
        } catch (Exception e){
            log.error("Error fetching and caching partners", e);
        }

    }



    //Fetch Partners
    private void fetchAndCacheParterns(){
        try{
            // Fetch partner MenuItems from the cache, if not found, fetch partners from API, convert to MenuItems, and cache them
            List<MenuItem> partnerMenuItems = cache.get("partners", k -> {
                List<Partner> partners = externalApiService.fetchAndCachePartners();
                if (partners == null || partners.isEmpty()) {
                    log.warn("No partners available to cache");
                    return List.of(); // Return an empty list if no partners are found
                }
                return IntStream.range(0, partners.size())
                        .mapToObj(i -> convertToMenuItem(partners.get(i), "PARTNER", i + 1))
                        .collect(Collectors.toList());
            });

            // Retrieve existing partner MenuItems from the database
            List<MenuItem> existingPartners = menuItemRepository.findByDynamicType("PARTNER");

            // Check if the number of cached partner MenuItems matches the number in the database
            if (existingPartners.size() != partnerMenuItems.size()) {
                log.info("PARTNER MenuItems missing or count mismatch (existing: {}, new: {}), updating...",
                        existingPartners.size(), partnerMenuItems.size());

                // Delete existing partner MenuItems from the database if they are outdated
                if (!existingPartners.isEmpty()) {
                    menuItemRepository.deleteByDynamicType("PARTNER");
                }

                // Save the new partner MenuItems to the database
                menuItemRepository.saveAll(partnerMenuItems);
                log.info("Cached {} partners as MenuItems", partnerMenuItems.size());
            } else {
                log.info("PARTNER MenuItems already exist and match count ({}), skipping update", existingPartners.size());
            }

        }catch (Exception e) {
            log.error("Error fetching and caching partners", e);
        }
    }





    private void fetchAndCacheClubs() {
        try {
            // Fetch club MenuItems from the cache, if not found, fetch clubs from API, convert to MenuItems, and cache them
            List<MenuItem> clubMenuItems = cache.get("clubs", k -> {
                List<Club> clubs = externalApiService.fetchAndCacheClubs();
                if (clubs == null || clubs.isEmpty()) {
                    log.warn("No clubs available to cache");
                    return List.of(); // Return an empty list if no clubs are found
                }
                return IntStream.range(0, clubs.size())
                        .mapToObj(i -> convertToMenuItem(clubs.get(i), "CLUB", i + 1))
                        .collect(Collectors.toList());
            });

            // Retrieve existing club MenuItems from the database
            List<MenuItem> existingClubs = menuItemRepository.findByDynamicType("CLUB");

            // Check if the number of cached club MenuItems matches the number in the database
            if (existingClubs.size() != clubMenuItems.size()) {
                log.info("CLUB MenuItems missing or count mismatch (existing: {}, new: {}), updating...",
                        existingClubs.size(), clubMenuItems.size());

                // Delete existing club MenuItems from the database if they are outdated
                if (!existingClubs.isEmpty()) {
                    menuItemRepository.deleteByDynamicType("CLUB");
                }

                // Save the new club MenuItems to the database
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
            // Fetch plan MenuItems from the cache, if not found, fetch plans from API, convert to MenuItems, and cache them
            List<MenuItem> planMenuItems = cache.get("plans", k -> {
                List<Plan> plans = null;
                int retryCount = 0;
                final int MAX_RETRIES = 3; // Set the maximum number of retries

                // Retry logic with exponential backoff
                while (plans == null && retryCount < MAX_RETRIES) {
                    try {
                        plans = externalApiService.fetchAndCachePlans();
                    } catch (IOException e) {
                        log.warn("Error fetching plans from API (attempt {}/{}): {}", retryCount + 1, MAX_RETRIES, e.getMessage());
                        retryCount++;
                        if (retryCount < MAX_RETRIES) {
                            try {
                                Thread.sleep((long) Math.pow(2, retryCount) * 1000); // Exponential backoff (e.g., 2s, 4s, 8s)
                            } catch (InterruptedException ex) {
                                log.error("Error during sleep", ex);
                                Thread.currentThread().interrupt();
                            }
                        }
                    }
                }

                if (plans == null || plans.isEmpty()) {
                    log.warn("No plans available to cache after {} retries", retryCount);
                    return List.of();
                }

                List<Plan> finalPlans = plans;
                return IntStream.range(0, plans.size())
                        .mapToObj(i -> convertToMenuItem(finalPlans.get(i), "PLAN", i + 1))
                        .collect(Collectors.toList());
            });

            // Retrieve existing plan MenuItems from the database
            List<MenuItem> existingPlans = menuItemRepository.findByDynamicType("PLAN");

            // Check if the number of cached plan MenuItems matches the number in the database
            if (existingPlans.size() != planMenuItems.size()) {
                log.info("PLAN MenuItems missing or count mismatch (existing: {}, new: {}), updating...",
                        existingPlans.size(), planMenuItems.size());

                // Delete existing plan MenuItems from the database if they are outdated
                if (!existingPlans.isEmpty()) {
                    menuItemRepository.deleteByDynamicType("PLAN");
                }

                // Save the new plan MenuItems to the database
                menuItemRepository.saveAll(planMenuItems);
                log.info("Cached {} plans as MenuItems", planMenuItems.size());
            } else {
                log.info("PLAN MenuItems already exist and match count ({}), skipping update", existingPlans.size());
            }
        } catch (Exception e) {
            log.error("Error fetching and caching plans", e);
        }

    }

    @Transactional
    private String handleVote(String phoneNumber, String[] inputs) {
        try {
            // The second last input represents the contestant selection
            int selectedPartner = Integer.parseInt(inputs[0]);
            int selectedPlanIndex = Integer.parseInt(inputs[inputs.length - 2]);
            int selectedContestantIndex = Integer.parseInt(inputs[inputs.length - 1]);

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
            Long menuId = LOCAL_PACKAGES_MENU_ID;

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

            // Retrieve Partner
            Long partnerMenuId = PARTNER_MENU_ID;

            List<MenuItem> partnerMenuItems = menuItemRepository.findByMenu_Id(partnerMenuId);
            MenuItem partnerItem = partnerMenuItems.stream()
                    .filter(item -> item.getText().startsWith(String.valueOf(selectedPartner)))
                    .findFirst()
                    .orElse(null);

            if (partnerItem == null) {
                return "Partner selection is invalid. Please try again.";
            }

            String partnerSelected = MenuItemParser.removeNumberPrefix(partnerItem.getText());

            // Prepare the vote request
            VoteRequestDTO voteRequest = new VoteRequestDTO();
            voteRequest.setContestantCode(votingCode); // Use the voting code from the Contestant table
            voteRequest.setPhoneNumber(phoneNumber.replace("+", "")); // Clean the phone number
            voteRequest.setChannel("USSD"); // Set the voting channel
            voteRequest.setAmount(amount);
            voteRequest.setPartnerCode(partnerSelected);

            // Log the details for debugging
            log.info("votingCode: {}, Processing voting of amount: {}, phoneNumber: {}, partnerCode: {}", votingCode, amount, phoneNumber, partnerSelected);

            // Call the external API to submit the vote
            VoteResponseDTO voteResponse = externalApiService.submitVote(voteRequest);

            if (voteResponse != null && voteResponse.getRespCode() == 2000) {
                return "Kamilisha Malipo kufanikisha Kura yako!";
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

    private MenuItem convertToMenuItem(Contestant contestant , String dynamicType, int index) {
        Menu menu = menuRepository.findById(DYNAMIC_CONTESTANT_MENU_ID)
                .orElseThrow(() -> new IllegalArgumentException("Invalid menu ID: " + DYNAMIC_CONTESTANT_MENU_ID));

        String menuItemText = index + ". " + contestant.getName();
        return new MenuItem(menuItemText, contestant.getId(), NEXT_MENU_ID, menu, dynamicType);
    }

    private MenuItem convertToMenuItem(Contest contest , String dynamicType, int index) {
        Menu menu = menuRepository.findById(DYNAMIC_CONTEST_MENU_ID)
                .orElseThrow(() -> new IllegalArgumentException("Invalid menu ID: " + DYNAMIC_CONTEST_MENU_ID));

        String menuItemText = index + ". " + contest.getName();
        return new MenuItem(menuItemText, contest.getId(), NEXT_MENU_ID, menu, dynamicType);
    }

    private MenuItem convertToMenuItem(Partner partner , String dynamicType, int index) {
        Menu menu = menuRepository.findById(PARTNER_MENU_ID)
                .orElseThrow(() -> new IllegalArgumentException("Invalid menu ID: " + PARTNER_MENU_ID));

        String menuItemText = index + ". " + partner.getName();
        return new MenuItem(menuItemText, partner.getId(), NEXT_MENU_ID, menu, dynamicType);
    }

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
        return selectedContest.getPlayerId(); // contestId
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
            int selectedPartner = Integer.parseInt(inputs[inputs.length - 3]);

            Long previousMenuId = DYNAMIC_MENU_ID;

            List<MenuItem> previousMenuItems = menuItemRepository.findByMenu_Id(previousMenuId);
            MenuItem selectedTeamItem = previousMenuItems.stream()
                    .filter(item -> item.getText().startsWith(String.valueOf(selectedClub)))
                    .findFirst()
                    .orElse(null);

            if (selectedTeamItem == null) {
                return "Chaguo sio Sahihi. Tafadhali jaribu Tena.";
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
                return "Chaguo sio Sahihi. Tafadhali jaribu Tena";
            }
            Long amount = currentItem.getAmount();

            // Retrieve Partner
            Long partnerMenuId = PARTNER_MENU_ID;

            List<MenuItem> partnerMenuItems = menuItemRepository.findByMenu_Id(partnerMenuId);
            MenuItem partnerItem = partnerMenuItems.stream()
                    .filter(item -> item.getText().startsWith(String.valueOf(selectedPartner)))
                    .findFirst()
                    .orElse(null);

            assert partnerItem != null;

            String partnerSelected = MenuItemParser.removeNumberPrefix(partnerItem.getText());


            // Log the details for debugging
            log.info("topicId: {}, Processing bundle of amount: {}, phoneNumber: {}, Partner: {}", menuItemId, amount, phoneNumber, partnerSelected);

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
            requestDTO.setPartnerCode(partnerSelected);

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