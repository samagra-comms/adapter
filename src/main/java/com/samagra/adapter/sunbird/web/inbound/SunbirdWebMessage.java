package com.samagra.adapter.sunbird.web.inbound;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.samagra.common.Request.CommonMessage;
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

    @JsonAlias({"Body"})
    String text;

    @Nullable
    String userId;

    String appId;

    String channel;

    @JsonAlias({"From"})
    String from;

    @Nullable
    Map<String,Object> context;

}
