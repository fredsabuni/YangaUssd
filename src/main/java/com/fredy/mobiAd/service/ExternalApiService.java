package com.fredy.mobiAd.service;

import com.fredy.mobiAd.dto.*;
import com.fredy.mobiAd.model.Player;
import com.fredy.mobiAd.repository.PlayerRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.annotation.Cacheable;
import jakarta.transaction.Transactional;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class ExternalApiService {

    private static final Logger log = LoggerFactory.getLogger(ExternalApiService.class);
    @Autowired
    private RestTemplate restTemplate;

    @Autowired
    private PlayerRepository playerRepository;

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

    public PaymentResponseDTO vote(PaymentRequestDTO paymentRequestDTO) {
        String url = voteBaseUrl + "/api/v1/contest/vote";
        return restTemplate.postForObject(url, paymentRequestDTO, PaymentResponseDTO.class);
    }

    public SubscriptionResponseDTO news(SubscriptionRequestDTO subscriptionRequestDTO){
        String url = newsBaseUrl + "/api/v1/subscriptions/subscribe";
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
}
