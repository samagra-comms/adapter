package com.uci.adapter.netcore.whatsapp.inbound;

import lombok.Getter;
import lombok.Setter;

import jakarta.xml.bind.annotation.XmlRootElement;

@Getter
@Setter
@XmlRootElement
public class TextType {
    private String text;
}
