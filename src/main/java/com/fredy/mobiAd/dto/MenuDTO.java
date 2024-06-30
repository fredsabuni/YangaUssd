package com.fredy.mobiAd.dto;

import java.util.List;

public class MenuDTO {
    private Long id;
    private String textEn;
    private String textSw;
    private Long parentId;
    private List <MenuItemDTO> menuItems;

    public MenuDTO(Long id, String textEn, String textSw, Long parentId, List<MenuItemDTO> menuItems) {
        this.id = id;
        this.textEn = textEn;
        this.textSw = textSw;
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

    public Long getParentId() {
        return parentId;
    }

    public void setParentId(Long parentId) {
        this.parentId = parentId;
    }
}
