package com.samagra.adapter.cdac;

import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;

import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlValue;

@NoArgsConstructor
@AllArgsConstructor
@JsonSerialize
public class PhoneNumberStatus {

    private  String mobileNumber;
    private  String status;

    @XmlAttribute(name="mobNo")
    public String getMobileNumber() {
        return mobileNumber;
    }

    public void setMobileNumber(String mobileNumber) {
        this.mobileNumber = mobileNumber;
    }
    @XmlValue
    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }
}
