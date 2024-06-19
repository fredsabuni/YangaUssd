package com.fredy.mobiAd.controller;

import com.fredy.mobiAd.dto.MenuDTO;
import com.fredy.mobiAd.dto.MenuItemDTO;
import com.fredy.mobiAd.model.Menu;
import com.fredy.mobiAd.service.MenuItemService;
import com.fredy.mobiAd.service.MenuService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Optional;

@RestController
@RequestMapping("/api/menus")
public class MenuController {
    @Autowired
    private MenuService menuService;

    @Autowired
    private MenuItemService menuItemService;

    @PostMapping
    public Menu createMenu(@RequestBody MenuDTO menuDTO) {
        return menuService.createMenu(menuDTO);
    }

    @PutMapping("/{id}")
    public Menu updateMenu(@PathVariable Long id, @RequestBody MenuDTO menuDTO) {
        return menuService.updateMenu(id, menuDTO);
    }

    @DeleteMapping("/{id}")
    public void deleteMenu(@PathVariable Long id) {
        menuService.deleteMenu(id);
    }

    @GetMapping
    public List<MenuDTO> getAllMenus() {
        return menuService.getAllMenus();
    }

    @GetMapping("/{id}")
    public Optional<Menu> getMenuById(@PathVariable Long id) {
        return menuService.getMenuById(id);
    }

    //MenuItems
    @PostMapping("/{menuId}/items")
    public MenuItemDTO createMenuItem(@PathVariable Long menuId, @RequestBody MenuItemDTO menuItemDTO) {
        menuItemDTO.setMenuId(menuId);
        return menuItemService.convertToDTO(menuItemService.createMenuItem(menuItemDTO));
    }

    @PutMapping("/items/{id}")
    public MenuItemDTO updateMenuItem(@PathVariable Long id, @RequestBody MenuItemDTO menuItemDTO) {
        return menuItemService.convertToDTO(menuItemService.updateMenuItem(id, menuItemDTO));
    }
}
