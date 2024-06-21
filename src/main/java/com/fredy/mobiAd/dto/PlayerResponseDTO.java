package com.fredy.mobiAd.dto;

import java.util.List;

public class PlayerResponseDTO {
    private boolean success;
    private String message;
    private Payload payload;

    public static class Payload {
        private List<PlayerDTO> players;

        // Getters and setters
        public List<PlayerDTO> getPlayers() {
            return players;
        }

        public void setPlayers(List<PlayerDTO> players) {
            this.players = players;
        }
    }

    public Payload getPayload() {
        return payload;
    }

    public void setPayload(Payload payload) {
        this.payload = payload;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public boolean isSuccess() {
        return success;
    }

    public void setSuccess(boolean success) {
        this.success = success;
    }
}
