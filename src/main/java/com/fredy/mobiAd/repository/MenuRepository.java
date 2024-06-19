package com.fredy.mobiAd.repository;

import com.fredy.mobiAd.model.Menu;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface MenuRepository extends JpaRepository<Menu, Long> {
    List<Menu> findByParentId(Long parentId);
}
