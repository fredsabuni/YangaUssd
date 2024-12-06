package com.fredy.mobiAd.dto;

public class SubscriptionResponseDTO {
    private boolean success;
    private String message;
    private Long respCode;

    public Long getRespCode() {
        return respCode;
    }

    public void setRespCode(Long respCode) {
        this.respCode = respCode;
    }

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
}
