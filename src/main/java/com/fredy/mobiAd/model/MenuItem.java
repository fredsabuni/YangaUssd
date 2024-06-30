package com.fredy.mobiAd.model;

import jakarta.persistence.*;

@Entity
public class MenuItem {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "menu_id", nullable = false)
    private Menu menu;

    private String textEn;
    private String textSw;

    @Column(nullable = true)
    private Long nextMenuId;

    private Long amount;

    private Long playerId;

    private String dynamicType;

    private Long referenceId;

    public String getDynamicType() {
        return dynamicType;
    }

    public void setDynamicType(String dynamicType) {
        this.dynamicType = dynamicType;
    }

    public Long getReferenceId() {
        return referenceId;
    }

    public void setReferenceId(Long referenceId) {
        this.referenceId = referenceId;
    }

    public Long getPlayerId() {
        return playerId;
    }

    public void setPlayerId(Long playerId) {
        this.playerId = playerId;
    }

    public Long getAmount() {
        return amount;
    }

    public void setAmount(Long amount) {
        this.amount = amount;
    }

    // Default constructor
    public MenuItem() {}

    public MenuItem(Long id, Menu menu, String textEn, String textSw, Long nextMenuId) {
        this.id = id;
        this.menu = menu;
        this.textEn = textEn;
        this.textSw = textSw;
        this.nextMenuId = nextMenuId;
    }

    public MenuItem(String textEn, String textSw, Long playerId, Long nextMenuId, Menu menu, String dynamicType) {
        this.textEn = textEn;
        this.textSw = textSw;
        this.nextMenuId = nextMenuId;
        this.playerId = playerId;
        this.menu = menu;
        this.dynamicType = dynamicType;
    }


    public MenuItem(Menu menu, String textEn, String textSw, Long nextMenuId, Long amount, Long playerId, String dynamicType, Long referenceId) {
        this.menu = menu;
        this.textEn = textEn;
        this.textSw = textSw;
        this.nextMenuId = nextMenuId;
        this.amount = amount;
        this.playerId = playerId;
        this.dynamicType = dynamicType;
        this.referenceId = referenceId;
    }

    public Long getNextMenuId() {
        return nextMenuId;
    }

    public void setNextMenuId(Long nextMenuId) {
        this.nextMenuId = nextMenuId;
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

    public Menu getMenu() {
        return menu;
    }

    public void setMenu(Menu menu) {
        this.menu = menu;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getMenuId() {
        return menu != null ? menu.getId() : null;
    }
}
