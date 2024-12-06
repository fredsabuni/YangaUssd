package com.fredy.mobiAd.dto;

import java.util.List;

public class ClubResponseDTO {

    private boolean success;
    private String message;
    private Long respCode;
    private List<ClubDTO> items;

    public List<ClubDTO> getItems() {
        return items;
    }

    public void setItems(List<ClubDTO> items) {
        this.items = items;
    }

    public Long getRespCode() {
        return respCode;
    }

    public void setRespCode(Long respCode) {
        this.respCode = respCode;
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
