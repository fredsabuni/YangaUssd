package com.fredy.mobiAd.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fredy.mobiAd.dto.*;
import com.fredy.mobiAd.model.*;
import com.fredy.mobiAd.repository.*;
import jakarta.servlet.http.Part;
import jakarta.transaction.Transactional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.io.IOException;
import java.util.List;
import java.util.stream.Collectors;


@Service
public class ExternalApiService {

    private static final Logger log = LoggerFactory.getLogger(ExternalApiService.class);
    @Autowired
    private RestTemplate restTemplate;

    @Autowired
    private PlayerRepository playerRepository;

    @Autowired
    private PlanRepository planRepository;

    @Autowired
    private ContestantRepository contestantRepository;

    @Autowired
    private ContestRepository contestRepository;

    @Autowired
    private ClubRepository clubRepository;

    @Autowired
    private PartnerRepository partnerRepository;

    @Value("${vote.base.url}")
    private String voteBaseUrl;

    @Value("${news.base.url}")
    private String newsBaseUrl;

    @Transactional
    @Cacheable("players")
    public List<Player> fetchAndCachePlayers() {
        String url = voteBaseUrl + "/api/v1/contest/players";
        PlayerResponseDTO response = restTemplate.getForObject(url, PlayerResponseDTO.class);

        if (response != null && response.isSuccess()) {
            List<PlayerDTO> playerDTOs = response.getPayload().getPlayers();
            List<Player> players = playerDTOs.stream().map(this::convertToEntity).collect(Collectors.toList());
            playerRepository.saveAll(players);
            return players;
        }

        return null;
    }


    @Transactional
    @Cacheable("partners")
    public List<Partner> fetchAndCachePartners() {
        String url = voteBaseUrl + "/api/v1/voting/partners";
        PartnerResponseDTO response = restTemplate.getForObject(url, PartnerResponseDTO.class);

        if (response != null && response.getRespCode() == 2000) {
            List<PartnerDTO> dtoDTOs = response.getItems();
            for (PartnerDTO partnerDTO : dtoDTOs) {
                log.info("PartnerDTO: {}", partnerDTO);
            }
            List<Partner> partners = dtoDTOs.stream().map(this::convertToEntity).collect(Collectors.toList());

            partnerRepository.deleteAll();
            List<Partner> savedPartners = partnerRepository.saveAll(partners);
            log.info("Saved {} partners to database", savedPartners.size());

            return savedPartners;
        }

        List<Partner> partnersFromDb = partnerRepository.findAll();
        if (!partnersFromDb.isEmpty()) {
            log.info("Returning clubs from database: {} partners found", partnersFromDb.size());
            return partnersFromDb; // Cached by @Cacheable
        }

        log.warn("No partners available from either API or database");
        return null;
    }



    @Transactional
    @Cacheable("clubs")
    public List<Club> fetchAndCacheClubs(){
        List<Club> clubsFromDb = clubRepository.findAll();
        if (!clubsFromDb.isEmpty()) {
            log.info("Returning clubs from database: {} clubs found", clubsFromDb.size());
            return clubsFromDb; // Cached by @Cacheable
        }

        String url = voteBaseUrl + "/api/v1/subscriptions/topics";
        ClubResponseDTO response = restTemplate.getForObject(url, ClubResponseDTO.class);

        if (response != null && response.getRespCode() == 2000) {
            List<ClubDTO> dtoDTOs = response.getItems();
            for (ClubDTO clubDTO : dtoDTOs) {
                log.info("ClubDTO: {}", clubDTO);
            }
            List<Club> clubs = dtoDTOs.stream().map(this::convertToEntity).collect(Collectors.toList());
            clubRepository.saveAll(clubs);
            return clubs;
        }

        return null;
    }


    @Transactional
    @Cacheable("plans")
    public List<Plan> fetchAndCachePlans() throws IOException {
        List<Plan> plansFromDb = planRepository.findAll();
        if (!plansFromDb.isEmpty()) {
            log.info("Returning plans from database: {} plans found", plansFromDb.size());
            return plansFromDb; // Cached by @Cacheable
        }

        ObjectMapper objectMapper = new ObjectMapper();
        ClassPathResource resource = new ClassPathResource("plan.json");
        PlanResponseDTO response = objectMapper.readValue(resource.getInputStream(), PlanResponseDTO.class);

        if (response != null && response.isSuccess()) {
            List<PlanDTO> plansDTOs = response.getPayload().getPlans();
            for (PlanDTO planDTO : plansDTOs) {
                log.info("PlanDTO: {}", planDTO);
            }
            List<Plan> plans = plansDTOs.stream()
                    .map(this::convertToEntity)
                    .collect(Collectors.toList());
            log.info("Plans fetched: {}", plans);
            planRepository.saveAll(plans);
            return plans;
        }

        return null;
    }

    @Transactional
    @Cacheable("goals")
    public List<Player> fetchAndCacheGoals() {
        String url = voteBaseUrl + "/api/v1/contest/goals";
        PlayerResponseDTO response = restTemplate.getForObject(url, PlayerResponseDTO.class);

        if (response != null && response.isSuccess()) {
            List<PlayerDTO> playerDTOs = response.getPayload().getPlayers();
            List<Player> players = playerDTOs.stream().map(this::convertToEntity).collect(Collectors.toList());
            playerRepository.saveAll(players);
            return players;
        }

        return null;
    }

    @Transactional
    @Cacheable("contests")
    public List<Contest> fetchContests() {
        List<Contest> contestsFromDb = contestRepository.findAll();
        if (!contestsFromDb.isEmpty()) {
            log.info("Returning contest from database: {} contest found", contestsFromDb.size());
            return contestsFromDb; // Cached by @Cacheable
        }

        String url = voteBaseUrl + "/api/v1/voting/contests";
        ContestResponseDTO response = restTemplate.getForObject(url, ContestResponseDTO.class);

        if (response != null && response.getRespCode() == 2000) {
            return response.getItems().stream().map(dto -> {
                Contest contest = new Contest();
                contest.setId(dto.getId());
                contest.setName(dto.getName());
                contest.setVotingCode(dto.getVotingCode());
                contest.setStatus(dto.getStatus());
                return contest;
            }).collect(Collectors.toList());
        }
        return List.of();
    }

    @Transactional
    public List<Contestant> fetchContestants(Long contestId) {
        try {
            List<Contestant> contestantsFromDb = contestantRepository.findAll();
            if (!contestantsFromDb.isEmpty()) {
                log.info("Returning contestants from database: {} contestants found", contestantsFromDb.size());
                return contestantsFromDb; // Cached by @Cacheable
            }

            // Fetch existing contestants for the given contestId and delete them
            List<Contestant> existingContestants = contestantRepository.findByContestId(contestId);
            if (!existingContestants.isEmpty()) {
                contestantRepository.deleteAll(existingContestants);
            }

            String url = voteBaseUrl + "/api/v1/voting/contestants?contestId={contestId}";
            ContestantResponseDTO response = restTemplate.getForObject(
                    url,
                    ContestantResponseDTO.class,
                    contestId
            );

            if (response != null && response.getRespCode() == 2000) {
                List<ContestantDTO> contestantDTOs = response.getItems();
                List<Contestant> contestants = contestantDTOs.stream()
                        .map(this::convertToEntity)
                        .collect(Collectors.toList());

                // Save the contestants to the database
                contestantRepository.saveAll(contestants);
                return contestants;
            } else {
                log.warn("No contestants found for contestId: {}", contestId);
            }
        } catch (Exception e) {
            log.error("Error fetching contestants for contestId: {}", contestId, e);
        }
        return List.of();
    }

    private Contestant convertToEntity(ContestantDTO contestantDTO) {
        Contestant contestant = new Contestant();
        contestant.setId(contestantDTO.getId());
        contestant.setName(contestantDTO.getName());
        contestant.setClub(contestantDTO.getClub());
        contestant.setContestId(contestantDTO.getContestId());
        contestant.setVotingCode(contestantDTO.getVotingCode());
        contestant.setStatus(contestantDTO.getStatus());
        return contestant;
    }


    public VoteResponseDTO submitVote(VoteRequestDTO voteRequest) {
        try {
            String url = voteBaseUrl + "/api/v1/voting/vote";
            return restTemplate.postForObject(url, voteRequest, VoteResponseDTO.class);
        } catch (Exception e) {
            log.error("Error submitting vote: ", e);
            return null;
        }
    }

    public PaymentResponseDTO vote(PaymentRequestDTO paymentRequestDTO) {
        String url = voteBaseUrl + "/api/v1/contest/vote";
        return restTemplate.postForObject(url, paymentRequestDTO, PaymentResponseDTO.class);
    }

    public SubscriptionResponseDTO news(SubscriptionRequestDTO subscriptionRequestDTO) {
        String url = voteBaseUrl + "/api/v1/subscriptions/subscribe";
        return restTemplate.postForObject(url, subscriptionRequestDTO, SubscriptionResponseDTO.class);
    }

    public List<Player> getPlayersFromCache(String type) {
        return playerRepository.findByType(type);
    }

    private Player convertToEntity(PlayerDTO playerDTO) {
        Player player = new Player();
        player.setId(playerDTO.getId());
        player.setCode(playerDTO.getCode());
        player.setName(playerDTO.getName());
        player.setClubCode(playerDTO.getClubCode());
        player.setClubName(playerDTO.getClubName());
        player.setVotesCount(playerDTO.getVotesCount());
        player.setTransactionsSum(playerDTO.getTransactionsSum());
        player.setActive(playerDTO.getIsActive() == 1);
        player.setColor(playerDTO.getColor());
        player.setClubColor(playerDTO.getClubColor());
        player.setType(playerDTO.getType());
        return player;
    }

    private Club convertToEntity(ClubDTO clubDTO) {
        Club club = new Club();
        club.setId(clubDTO.getId());
        club.setClubName(clubDTO.getName());
        club.setCreatedAt(clubDTO.getCreated_at());
        club.setUpdatedAt(clubDTO.getUpdated_at());
        return club;
    }


    private Partner convertToEntity(PartnerDTO partnerDTO) {
        Partner partner = new Partner();
        partner.setName(partnerDTO.getName());
        partner.setCode(partnerDTO.getCode());
        return partner;
    }

    private Plan convertToEntity(PlanDTO planDTO) {
        log.info("Converting PlanDTO: {}", planDTO);

        Plan plan = new Plan();
        plan.setId(planDTO.getId());
        plan.setName(planDTO.getName());
        if (planDTO.getAmount() != null) {
            plan.setAmount(planDTO.getAmount());
            log.info("Amount set: {}", plan.getAmount());
        }
        plan.setCreatedAt(planDTO.getCreatedAt());
        plan.setUpdatedAt(planDTO.getUpdatedAt());

        return plan;
    }
}
