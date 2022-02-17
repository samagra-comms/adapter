package com.uci.adapter.netcore.whatsapp.outbound.interactive;

import java.util.ArrayList;

import javax.xml.bind.annotation.XmlRootElement;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.sun.istack.Nullable;
import com.uci.adapter.netcore.whatsapp.outbound.SingleMessage.SingleMessageBuilder;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@XmlRootElement
@Builder
public class InteractiveContent {
    private String type;
    
    private String body;
    
    private Action[] action;
}
