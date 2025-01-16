package com.fredy.mobiAd.dto;

public class ContestantDTO {
    private Long id;
    private String name;
    private String club;
    private String votingCode;
    private Long contestId;
    private String status;
    private String createdDate;

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getClub() {
        return club;
    }

    public void setClub(String club) {
        this.club = club;
    }

    public String getVotingCode() {
        return votingCode;
    }

    public void setVotingCode(String votingCode) {
        this.votingCode = votingCode;
    }

    public Long getContestId() {
        return contestId;
    }

    public void setContestId(Long contestId) {
        this.contestId = contestId;
    }
}
