package com.samagra.adapter.netcore.whatsapp.inbound;

import lombok.Getter;
import lombok.Setter;

import javax.xml.bind.annotation.XmlRootElement;

@Getter
@Setter
@XmlRootElement
public class TextType {
    private String text;
}
