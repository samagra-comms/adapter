package com.samagra.adapter.sunbird.web.outbound;

import lombok.*;

import javax.xml.bind.annotation.XmlRootElement;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@XmlRootElement
@Builder
public class SunbirdMessage {
    private String text;
}
