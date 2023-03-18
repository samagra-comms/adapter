package com.uci.adapter.gs.whatsapp;

import lombok.Getter;
import lombok.Setter;

import jakarta.xml.bind.annotation.XmlRootElement;

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
    private String extra;
}
