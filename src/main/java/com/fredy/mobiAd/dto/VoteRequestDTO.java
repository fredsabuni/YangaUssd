package com.fredy.mobiAd.dto;

public class VoteRequestDTO {
    private String contestantCode; // Voting code
    private String phoneNumber;
    private String channel;
    private Long amount;
    private String partnerCode;

    public String getPartnerCode() {
        return partnerCode;
    }

    public void setPartnerCode(String partnerCode) {
        this.partnerCode = partnerCode;
    }

    public String getContestantCode() {
        return contestantCode;
    }

    public void setContestantCode(String contestantCode) {
        this.contestantCode = contestantCode;
    }

    public String getPhoneNumber() {
        return phoneNumber;
    }

    public void setPhoneNumber(String phoneNumber) {
        this.phoneNumber = phoneNumber;
    }

    public String getChannel() {
        return channel;
    }

    public void setChannel(String channel) {
        this.channel = channel;
    }

    public Long getAmount() {
        return amount;
    }

    public void setAmount(Long amount) {
        this.amount = amount;
    }
}
