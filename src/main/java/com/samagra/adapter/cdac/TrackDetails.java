package com.samagra.adapter.cdac;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import lombok.*;

import javax.persistence.Entity;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;
import java.io.Serializable;

@Data
@XmlRootElement(name="dept")
public class TrackDetails implements Serializable {

    @XmlElement(name="msgid")
    String messageId;

    @XmlElement(name="delvSMSCount")
    String deliveredSMSCount;

    @XmlElement(name="fldSMSCount")
    String failedSMSCount;

    @XmlElement(name="subSMSCount")
    String submittedSMSCount;

    @XmlElement(name="undelv")
    String undelivered;

    @XmlElement(name="del")
    DeliveryDetails delivered;

    @XmlElement(name="sub")
    DeliveryDetails submitted;

    @XmlElement(name="fld")
    DeliveryDetails failed;

}
