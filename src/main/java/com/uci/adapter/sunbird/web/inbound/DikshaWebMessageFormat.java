package com.uci.adapter.sunbird.web.inbound;

import com.fasterxml.jackson.annotation.JsonAlias;
import lombok.Getter;
import lombok.Setter;

import javax.xml.bind.annotation.XmlRootElement;

@Getter
@Setter
@XmlRootElement
public class DikshaWebMessageFormat {

    @JsonAlias({"incoming_message", "delivery_status"})
    private SunbirdWebMessage[] messages;
}
