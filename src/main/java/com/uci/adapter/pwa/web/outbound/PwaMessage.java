package com.uci.adapter.pwa.web.outbound;

import lombok.*;
import messagerosa.core.model.*;

import jakarta.xml.bind.annotation.XmlRootElement;
import java.util.ArrayList;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@XmlRootElement
@Builder
public class PwaMessage {
    private String title;
    private ArrayList<ButtonChoice> choices;
    private String media_url;
    private String caption;
    private String msg_type;
}

