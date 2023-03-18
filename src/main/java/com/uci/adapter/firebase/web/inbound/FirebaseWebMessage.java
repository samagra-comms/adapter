package com.uci.adapter.firebase.web.inbound;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.uci.adapter.Request.CommonMessage;
import com.sun.istack.Nullable;
import lombok.Getter;
import lombok.Setter;
import jakarta.xml.bind.annotation.XmlRootElement;

@Getter
@Setter
@XmlRootElement
public class FirebaseWebMessage extends CommonMessage {

    String messageId;

    String text;

    @JsonAlias({"From"})
    String from;

    @Nullable
    String eventType;

    @Nullable
    FirebaseWebReport report;
}

