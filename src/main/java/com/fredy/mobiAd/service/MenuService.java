package com.fredy.mobiAd.service;


import com.fredy.mobiAd.dto.MenuDTO;
import com.fredy.mobiAd.dto.MenuItemDTO;
import com.fredy.mobiAd.model.Menu;
import com.fredy.mobiAd.model.MenuItem;
import com.fredy.mobiAd.repository.MenuRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
public class MenuService {

    @Autowired
    private MenuRepository menuRepository;

    public Menu createMenu(MenuDTO menuDTO) {
        Menu menu = new Menu();
        menu.setText(menuDTO.getText());
        menu.setParentId(menuDTO.getParentId());
        return menuRepository.save(menu);
    }

    public Menu updateMenu(Long id, MenuDTO menuDTO) {
        Menu menu = menuRepository.findById(id).orElseThrow(() -> new IllegalArgumentException("Invalid menu ID: " + id));
        menu.setText(menuDTO.getText());
        menu.setParentId(menuDTO.getParentId());
        return menuRepository.save(menu);
    }

    public void deleteMenu(Long id) {
        menuRepository.deleteById(id);
    }

    public List<MenuDTO> getAllMenus() {
        return menuRepository.findAll().stream().map(this::convertToDTO).collect(Collectors.toList());
    }

    private MenuDTO convertToDTO(Menu menu) {
        List<MenuItemDTO> menuItemDTOS = menu.getMenuItems().stream()
                .map(menuItem -> new MenuItemDTO(menuItem.getId(), menuItem.getText(), menuItem.getAmount(), menuItem.getNextMenuId(), menu.getId()))
                .collect(Collectors.toList());

        return new MenuDTO(menu.getId(), menu.getText(), menu.getParentId(), menuItemDTOS);
    }

    public Optional<Menu> getMenuById(Long id) {
        return menuRepository.findById(id);
    }
}
