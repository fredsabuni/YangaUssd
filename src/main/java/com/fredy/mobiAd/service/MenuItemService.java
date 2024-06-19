package com.fredy.mobiAd.service;

import com.fredy.mobiAd.dto.MenuItemDTO;
import com.fredy.mobiAd.repository.MenuItemRepository;
import com.fredy.mobiAd.model.MenuItem;
import com.fredy.mobiAd.repository.MenuRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
public class MenuItemService {
    @Autowired
    private MenuItemRepository menuItemRepository;

    @Autowired
    private MenuRepository menuRepository;

    public MenuItem createMenuItem(MenuItemDTO menuItemDTO) {
        MenuItem menuItem = new MenuItem();
        menuItem.setText(menuItemDTO.getText());
        menuItem.setAmount(menuItemDTO.getAmount());
        menuItem.setNextMenuId(menuItemDTO.getNextMenuId());
        menuItem.setMenu(menuRepository.findById(menuItemDTO.getMenuId())
                .orElseThrow(() -> new IllegalArgumentException("Invalid menu ID: " + menuItemDTO.getMenuId())));
        return menuItemRepository.save(menuItem);
    }

    public MenuItem updateMenuItem(Long id, MenuItemDTO menuItemDTO) {
        MenuItem menuItem = menuItemRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Invalid menu item ID: " + id));
        menuItem.setText(menuItemDTO.getText());
        menuItem.setAmount(menuItemDTO.getAmount());
        menuItem.setNextMenuId(menuItemDTO.getNextMenuId());
        menuItem.setMenu(menuRepository.findById(menuItemDTO.getMenuId())
                .orElseThrow(() -> new IllegalArgumentException("Invalid menu ID: " + menuItemDTO.getMenuId())));
        return menuItemRepository.save(menuItem);
    }

    public void deleteMenuItem(Long id) {
        menuItemRepository.deleteById(id);
    }

    public List<MenuItem> getAllMenuItems() {
        return menuItemRepository.findAll();
    }

    public Optional<MenuItem> getMenuItemById(Long id) {
        return menuItemRepository.findById(id);
    }

    public MenuItemDTO convertToDTO(MenuItem menuItem) {
        return new MenuItemDTO(menuItem.getId(), menuItem.getText(), menuItem.getAmount(), menuItem.getNextMenuId(), menuItem.getMenuId());
    }
}
