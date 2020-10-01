package com.samagra.adapter.cdac;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import lombok.*;

import javax.xml.bind.annotation.XmlElement;
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
