package com.fredy.mobiAd.dto;

public class MenuItemDTO {
    private Long id;
    private Long menuId;
    private String text;
    private Long amount;
    private Long nextMenuId;


    public MenuItemDTO(Long id, String text, Long amount, Long nextMenuId, Long menuId) {
        this.id = id;
        this.text = text;
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

    public String getText() {
        return text;
    }

    public void setText(String text) {
        this.text = text;
    }

    public Long getNextMenuId() {
        return nextMenuId;
    }

    public void setNextMenuId(Long nextMenuId) {
        this.nextMenuId = nextMenuId;
    }
}
