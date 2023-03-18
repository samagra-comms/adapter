package com.uci.adapter.netcore.whatsapp.outbound.interactive;

import jakarta.xml.bind.annotation.XmlRootElement;

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
