package com.samagra.adapter.gs.whatsapp;

import lombok.Getter;
import lombok.Setter;

import javax.xml.bind.annotation.XmlRootElement;

@Getter
@Setter
@XmlRootElement
public class GSWhatsappReport {
    private String externalId;
    private String eventType;
    private String eventTs;
    private String destAddr;
    private String srcAddr;
    private String cause;
    private String errorCode;
    private String channel;

}
