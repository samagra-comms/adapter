package com.samagra.notification.Response;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.samagra.common.Request.MessagePayload;
import com.samagra.common.Request.Sender;
import com.samagra.notification.Enums.PayLoadType;
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
public class PayloadResponse {
  private String id;
  private String source;
  @JsonProperty
  private PayLoadType type;
  @JsonProperty
  private MessagePayload payload;
  @JsonProperty
  private Sender sender;
}
