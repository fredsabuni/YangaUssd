package com.fredy.mobiAd.repository;

import com.fredy.mobiAd.model.Contestant;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ContestantRepository extends JpaRepository<Contestant, Long> {
}

