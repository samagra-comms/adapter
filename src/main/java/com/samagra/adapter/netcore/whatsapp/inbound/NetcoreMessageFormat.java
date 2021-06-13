package com.samagra.adapter.netcore.whatsapp.inbound;

import com.fasterxml.jackson.annotation.JsonAlias;
import lombok.Getter;
import lombok.Setter;

import javax.xml.bind.annotation.XmlRootElement;

@Getter
@Setter
@XmlRootElement
public class NetcoreMessageFormat {

    @JsonAlias({"incoming_message", "delivery_status"})
    private NetcoreWhatsAppMessage[] messages;
}
