package com.uci.adapter.netcore.whatsapp.inbound;

import com.sun.istack.Nullable;
import lombok.Getter;
import lombok.Setter;

import javax.xml.bind.annotation.XmlRootElement;

@Getter
@Setter
@XmlRootElement
public class MessageContext {

    @Nullable
    private String ncmessage_id;

    @Nullable
    private String message_id;

}
