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

    @XmlAttribute(name="mobNo")
    protected String mobileNumber;

    @XmlValue
    protected String status;


}
