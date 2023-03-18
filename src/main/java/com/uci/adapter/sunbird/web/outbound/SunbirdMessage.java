package com.uci.adapter.sunbird.web.outbound;

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
public class SunbirdMessage {

    private String title;
    private ArrayList<ButtonChoice> choices;
}
