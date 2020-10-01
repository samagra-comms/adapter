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

    @XmlElement(name="no")
    List<PhoneNumberStatus> phoneNumberStatus;

}
