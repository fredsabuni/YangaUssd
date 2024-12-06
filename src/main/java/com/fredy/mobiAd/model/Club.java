package com.fredy.mobiAd.model;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;

import java.time.LocalDateTime;


@Entity
public class Club {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String clubName; 
    private String createdAt;
    private String updatedAt;


    public Club(){}

    public Long getId() {
        return id;
    }

    public String getClubName() {
        return clubName;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public void setClubName(String clubName) {
        this.clubName = clubName;
    }

    public String getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(String createdAt) {
        this.createdAt = createdAt;
    }

    public String getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(String updatedAt) {
        this.updatedAt = updatedAt;
    }
}
