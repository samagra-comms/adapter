package com.samagra.notification.Response;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;

@AllArgsConstructor
public class WrapperResponse {
  @Builder.Default
  @JsonProperty(value = "200")
  private int code = 200;

  @Builder.Default
  @JsonProperty(value = "status_message")
  private String statusMessage = "OK";

}
