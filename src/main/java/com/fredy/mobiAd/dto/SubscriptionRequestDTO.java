package com.fredy.mobiAd.dto;

public class SubscriptionRequestDTO {
    private String topicId;
    private String subscriptionPhone;
    private String paymentPhone;
    private Long amount;
    private String channel;


    public String getTopicId() {
        return topicId;
    }

    public void setTopicId(String topicId) {
        this.topicId = topicId;
    }

    public String getSubscriptionPhone() {
        return subscriptionPhone;
    }

    public void setSubscriptionPhone(String subscriptionPhone) {
        this.subscriptionPhone = subscriptionPhone;
    }

    public String getPaymentPhone() {
        return paymentPhone;
    }

    public void setPaymentPhone(String paymentPhone) {
        this.paymentPhone = paymentPhone;
    }

    public Long getAmount() {
        return amount;
    }

    public void setAmount(Long amount) {
        this.amount = amount;
    }

    public String getChannel() {
        return channel;
    }

    public void setChannel(String channel) {
        this.channel = channel;
    }
}
