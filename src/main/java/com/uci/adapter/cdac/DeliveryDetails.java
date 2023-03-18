package com.uci.adapter.cdac;

import lombok.*;

import jakarta.xml.bind.annotation.XmlElement;
import java.util.List;

@Data
public class DeliveryDetails {
    private List<PhoneNumberStatus> phoneNumberStatus;

    @XmlElement(name="no")
    public List<PhoneNumberStatus> getPhoneNumberStatus() {
        return phoneNumberStatus;
    }

    public void setPhoneNumberStatus(List<PhoneNumberStatus> phoneNumberStatus) {
        this.phoneNumberStatus = phoneNumberStatus;
    }

}
