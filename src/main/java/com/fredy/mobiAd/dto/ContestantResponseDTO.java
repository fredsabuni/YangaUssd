package com.fredy.mobiAd.dto;

import java.util.List;

public class ContestantResponseDTO {
    private int respCode;
    private String message;
    private List<ContestantDTO> items;

    public int getRespCode() {
        return respCode;
    }

    public void setRespCode(int respCode) {
        this.respCode = respCode;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public List<ContestantDTO> getItems() {
        return items;
    }

    public void setItems(List<ContestantDTO> items) {
        this.items = items;
    }
}
