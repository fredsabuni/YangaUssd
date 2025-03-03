package com.fredy.mobiAd.dto;

import java.util.List;

public class PartnerResponseDTO {
    private int respCode;
    private String message;
    private int currentPageElements;
    private List<PartnerDTO> items;

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

    public int getCurrentPageElements() {
        return currentPageElements;
    }

    public void setCurrentPageElements(int currentPageElements) {
        this.currentPageElements = currentPageElements;
    }

    public List<PartnerDTO> getItems() {
        return items;
    }

    public void setItems(List<PartnerDTO> items) {
        this.items = items;
    }
}
