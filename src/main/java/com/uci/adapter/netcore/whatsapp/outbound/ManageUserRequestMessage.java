package com.uci.adapter.netcore.whatsapp.outbound;

import lombok.Getter;
import lombok.Setter;

import javax.xml.bind.annotation.XmlRootElement;

@Getter
@Setter
@XmlRootElement
public class ManageUserRequestMessage {

    @Getter
    @Setter
    public class Recipient {
        private String recipient;
        private String source;
    }

    private String type;
    private Recipient[] recipients;
}
