package com.samagra.common.Request;

import javax.xml.bind.annotation.XmlRootElement;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.sun.istack.NotNull;
import com.sun.istack.Nullable;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@XmlRootElement
public class Message {
  private String app;

  @Nullable
  private Long timestamp;
  @Nullable
  private int version;

  @JsonProperty
  private String type;

  @NotNull
  @JsonProperty
  private MsgPayload payload;
}
