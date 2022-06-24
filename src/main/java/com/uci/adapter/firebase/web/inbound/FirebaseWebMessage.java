package com.uci.adapter.firebase.web.inbound;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.uci.adapter.Request.CommonMessage;
import com.sun.istack.Nullable;
import lombok.Getter;
import lombok.Setter;
import javax.xml.bind.annotation.XmlRootElement;
import java.util.Map;

@Getter
@Setter
@XmlRootElement
public class FirebaseWebMessage extends CommonMessage {

    String messageId;

    String text;

    @JsonAlias({"From"})
    String from;

    @Nullable
    String fcmToken;

    @Nullable
    String eventType;
}

