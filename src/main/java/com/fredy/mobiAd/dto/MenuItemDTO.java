package com.fredy.mobiAd.dto;

public class MenuItemDTO {
    private Long id;
    private Long menuId;
    private String textEn;
    private String textSw;
    private Long amount;
    private Long nextMenuId;


    public MenuItemDTO(Long id, String textEn, String textSw, Long amount, Long nextMenuId,Long menuId) {
        this.id = id;
        this.textEn = textEn;
        this.textSw = textSw;
        this.amount = amount;
        this.nextMenuId = nextMenuId;
        this.menuId = menuId;
    }

    public Long getAmount() {
        return amount;
    }

    public void setAmount(Long amount) {
        this.amount = amount;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getMenuId() {
        return menuId;
    }

    public void setMenuId(Long menuId) {
        this.menuId = menuId;
    }

    public String getTextEn() {
        return textEn;
    }

    public void setTextEn(String textEn) {
        this.textEn = textEn;
    }

    public String getTextSw() {
        return textSw;
    }

    public void setTextSw(String textSw) {
        this.textSw = textSw;
    }

    public Long getNextMenuId() {
        return nextMenuId;
    }

    public void setNextMenuId(Long nextMenuId) {
        this.nextMenuId = nextMenuId;
    }
}
