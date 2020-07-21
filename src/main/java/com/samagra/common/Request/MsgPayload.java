package com.samagra.common.Request;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.sun.istack.Nullable;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Builder
@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
public class MsgPayload {
  @Nullable
  private String id;
  private String source;
  @JsonProperty
  private String type;
  @JsonProperty
  private MessagePayload payload;
  @JsonProperty
  @Nullable
  private Sender sender;
  @Nullable
  private String destination;
  @Nullable
  private String gsId;
  @Nullable
  private String phone;

}
