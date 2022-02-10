package com.uci.adapter.netcore.whatsapp.outbound;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.sun.istack.Nullable;
import lombok.*;

import javax.xml.bind.annotation.XmlRootElement;

@Getter
@Setter
@XmlRootElement
@Builder
public  class SingleMessage {

    @JsonProperty("recipient_whatsapp")
    @JsonAlias({"recipient_whatsapp"})
    private String to;

    @JsonProperty("source")
    @JsonAlias({"source"})
    private String from;

    @JsonProperty("recipient_type")
    @JsonAlias({"recipient_type"})
    private String recipientType;

    @JsonProperty("message_type")
    @JsonAlias({"message_type"})
    private String messageType;

    @JsonProperty("x-apiheader")
    @JsonAlias({"x-apiheader"})
    private String header;

    @Nullable
    @JsonProperty("type_text")
    @JsonAlias({"type_text"})
    private Text[] text;
    
    @Nullable
    @JsonProperty("type_interactive")
    @JsonAlias({"type_interactive"})
    private InterativeContent[] interativeContent;

    private String details;
    private String status;
}
