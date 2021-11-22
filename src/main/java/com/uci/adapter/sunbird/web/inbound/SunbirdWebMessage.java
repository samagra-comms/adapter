package com.uci.adapter.sunbird.web.inbound;

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
public class SunbirdWebMessage extends CommonMessage {

    String messageId;

    @JsonAlias({"body"})
    String text;

    @Nullable
    String userId;

    String appId;

    String channel;

    @JsonAlias({"From"})
    String from;

    @JsonAlias({"to"})
    String to;

    @Nullable
    Map<String,Object> context;

}
