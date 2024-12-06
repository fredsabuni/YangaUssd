package com.fredy.mobiAd.dto;

import java.util.List;

public class PlanResponseDTO {

    private boolean success;
    private String message;
    private Payload payload;

    public static class Payload {
        private List<PlanDTO> plans;

        // Getters and Setters
        public List<PlanDTO> getPlans() {
            return plans;
        }

        public void setPlans(List<PlanDTO> plans) {
            this.plans = plans;
        }
    }

    // Getters and Setters for PlanResponseDTO

    public boolean isSuccess() {
        return success;
    }

    public void setSuccess(boolean success) {
        this.success = success;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public Payload getPayload() {
        return payload;
    }

    public void setPayload(Payload payload) {
        this.payload = payload;
    }
}
