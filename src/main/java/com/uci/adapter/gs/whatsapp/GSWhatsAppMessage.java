package com.uci.adapter.gs.whatsapp;

import jakarta.xml.bind.annotation.XmlRootElement;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.uci.adapter.Request.CommonMessage;
import com.sun.istack.Nullable;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@XmlRootElement
public class GSWhatsAppMessage extends CommonMessage {
  private String waNumber;
  private String mobile;
  private String replyId;
  private String messageId;
  @Nullable
  private Long timestamp;
  private String name;
  @Nullable
  private int version;
  @JsonProperty
  private String type;
  private String text;
  private String image;
  private String document;
  private String voice;
  private String audio;
  private String video;
  private String location;
  private String response;
  private String extra;
  private String app;
//  private JSONObject contacts;
//
//  @NotNull
//  @JsonProperty
//  private MsgPayload payload;
  
  @Nullable
  @JsonAlias({"interactive"})
  private String interactive; 
}
