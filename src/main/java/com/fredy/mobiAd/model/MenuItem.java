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

    private String text;

    @Column(nullable = true)
    private Long nextMenuId;

    private Long amount;

    public Long getAmount() {
        return amount;
    }

    public void setAmount(Long amount) {
        this.amount = amount;
    }

    // Default constructor
    public MenuItem() {}

    public MenuItem(Long id, Menu menu, String text, Long nextMenuId) {
        this.id = id;
        this.menu = menu;
        this.text = text;
        this.nextMenuId = nextMenuId;
    }

    public Long getNextMenuId() {
        return nextMenuId;
    }

    public void setNextMenuId(Long nextMenuId) {
        this.nextMenuId = nextMenuId;
    }

    public String getText() {
        return text;
    }

    public void setText(String text) {
        this.text = text;
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
