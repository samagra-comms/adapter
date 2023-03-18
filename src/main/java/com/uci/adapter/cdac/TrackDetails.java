package com.uci.adapter.cdac;

import lombok.*;

import jakarta.xml.bind.annotation.XmlElement;
import jakarta.xml.bind.annotation.XmlRootElement;

@Data
@XmlRootElement(name = "dept")
public class TrackDetails {
    private String messageId;
    private String deliveredSMSCount;
    private String failedSMSCount;
    private String submittedSMSCount;
    private String undelivered;
    private DeliveryDetails delivered;
    private DeliveryDetails submitted;
    private DeliveryDetails failed;


    @XmlElement(name = "msgid")
    public String getMessageId() {
        return messageId;
    }

    public void setMessageId(String messageId) {
        this.messageId = messageId;
    }

    @XmlElement(name = "delvSMSCount")
    public String getDeliveredSMSCount() {
        return deliveredSMSCount;
    }

    public void setDeliveredSMSCount(String deliveredSMSCount) {
        this.deliveredSMSCount = deliveredSMSCount;
    }

    @XmlElement(name = "fldSMSCount")
    public String getFailedSMSCount() {
        return failedSMSCount;
    }

    public void setFailedSMSCount(String failedSMSCount) {
        this.failedSMSCount = failedSMSCount;
    }

    @XmlElement(name = "subSMSCount")
    public String getSubmittedSMSCount() {
        return submittedSMSCount;
    }

    public void setSubmittedSMSCount(String submittedSMSCount) {
        this.submittedSMSCount = submittedSMSCount;
    }

    @XmlElement(name = "undelv")
    public String getUndelivered() {
        return undelivered;
    }

    public void setUndelivered(String undelivered) {
        this.undelivered = undelivered;
    }

    @XmlElement(name = "del")
    public DeliveryDetails getDelivered() {
        return delivered;
    }

    public void setDelivered(DeliveryDetails delivered) {
        this.delivered = delivered;
    }

    @XmlElement(name = "sub")
    public DeliveryDetails getSubmitted() {
        return submitted;
    }

    public void setSubmitted(DeliveryDetails submitted) {
        this.submitted = submitted;
    }

    @XmlElement(name = "fld")
    public DeliveryDetails getFailed() {
        return failed;
    }

    public void setFailed(DeliveryDetails failed) {
        this.failed = failed;
    }



}
