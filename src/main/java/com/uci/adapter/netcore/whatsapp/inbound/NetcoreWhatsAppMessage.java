package com.uci.adapter.netcore.whatsapp.inbound;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.uci.adapter.Request.CommonMessage;
import com.sun.istack.Nullable;
import lombok.Getter;
import lombok.Setter;

import javax.xml.bind.annotation.XmlRootElement;

@Getter
@Setter
@XmlRootElement
public class NetcoreWhatsAppMessage extends CommonMessage {

  private String waNumber;

  @JsonAlias({"from", "recipient"})
  private String mobile;

  private String replyId;

  @JsonAlias({"message_id", "ncmessage_id"})
  private String messageId;

  @JsonAlias({"received_at"})
  private String timestamp;

  @Nullable
  private String name;

  @Nullable
  private int version;

  @Nullable
  @JsonAlias({"message_type"})
  private String type;

  @Nullable
  @JsonAlias({"text_type"})
  private TextType text;

  @Nullable
  @JsonAlias({"status"})
  private String eventType;

  @Nullable
  private MessageContext context;

  @Nullable
  @JsonAlias({"status_remark"})
  private String statusRemark;

  @Nullable
  @JsonAlias({"source"})
  private String source;

  @Nullable
  private NetcoreInboundFile image;
  private NetcoreInboundFile document;
  private NetcoreInboundFile voice;
  private NetcoreInboundFile audio;
  private NetcoreInboundFile video;
  private String location;
  private String response;
  private String extra;
  private String app;
  
  @Nullable
  @JsonAlias({"interactive_type"})
  private InteractiveContent interativeContent; 
}
