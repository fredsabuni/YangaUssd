package com.fredy.mobiAd.repository;

import com.fredy.mobiAd.model.MenuItem;
import jakarta.transaction.Transactional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface MenuItemRepository extends JpaRepository<MenuItem, Long> {
    List<MenuItem> findByMenu_Id(Long menuId);

    List<MenuItem> findByDynamicType(String dynamicType);

    @Modifying
    @Transactional
    @Query("DELETE FROM MenuItem m WHERE m.dynamicType = :dynamicType")
    void deleteByDynamicType(@Param("dynamicType") String dynamicType);

}
