package com.uci.adapter.netcore.whatsapp.inbound;

import com.fasterxml.jackson.annotation.JsonAlias;
import lombok.Getter;
import lombok.Setter;

import jakarta.xml.bind.annotation.XmlRootElement;

@Getter
@Setter
@XmlRootElement
public class NetcoreMessageFormat {

    @JsonAlias({"incoming_message", "delivery_status"})
    private NetcoreWhatsAppMessage[] messages;
}
