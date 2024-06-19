package com.fredy.mobiAd.dto;

import java.util.List;

public class MenuDTO {
    private Long id;
    private String text;
    private Long parentId;
    private List <MenuItemDTO> menuItems;

    public MenuDTO(Long id, String text, Long parentId, List<MenuItemDTO> menuItems) {
        this.id = id;
        this.text = text;
        this.parentId = parentId;
        this.menuItems = menuItems;
    }

    public List<MenuItemDTO> getMenuItems() {
        return menuItems;
    }

    public void setMenuItems(List<MenuItemDTO> menuItems) {
        this.menuItems = menuItems;
    }

    public Long getId() {
        return id;
    }


    public void setId(Long id) {
        this.id = id;
    }

    public String getText() {
        return text;
    }

    public void setText(String text) {
        this.text = text;
    }

    public Long getParentId() {
        return parentId;
    }

    public void setParentId(Long parentId) {
        this.parentId = parentId;
    }
}
