package com.fredy.mobiAd.repository;

import com.fredy.mobiAd.model.UserSessionLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface UserSessionLogRepository extends JpaRepository<UserSessionLog, Long> {
}
