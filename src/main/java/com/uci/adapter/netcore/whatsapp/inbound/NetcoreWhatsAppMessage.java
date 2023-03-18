package com.uci.adapter.netcore.whatsapp.inbound;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.uci.adapter.Request.CommonMessage;
import com.sun.istack.Nullable;
import lombok.Getter;
import lombok.Setter;

import jakarta.xml.bind.annotation.XmlRootElement;

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
  @JsonAlias({"image_type"})
  private NetcoreInboundFile imageType;
 
  @JsonAlias({"document_type"})
  private NetcoreInboundFile documentType;
  
  @JsonAlias({"voice_type"})
  private NetcoreInboundFile voiceType;
  
  @JsonAlias({"audio_type"})
  private NetcoreInboundFile audioType;
  
  @JsonAlias({"video_type"})
  private NetcoreInboundFile videoType;
  
//  private String location;
  private String response;
  private String extra;
  private String app;
  
  @Nullable
  @JsonAlias({"location_type"})
  private NetcoreLocation location;
  
  @Nullable
  @JsonAlias({"interactive_type"})
  private InteractiveContent interativeContent; 
}
