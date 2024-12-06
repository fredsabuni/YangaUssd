package com.fredy.mobiAd.repository;

import com.fredy.mobiAd.model.Club;
 import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ClubRepository extends JpaRepository<Club, Long> {
}
