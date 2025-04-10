package com.fredy.mobiAd.repository;

 import com.fredy.mobiAd.model.Partner;
 import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface PartnerRepository extends JpaRepository<Partner, Long>  {
}

