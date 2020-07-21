package com.samagra.common.Request;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
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
public class MessagePayload {
  @Nullable
  @JsonInclude(Include.NON_NULL)
  private String text;
  @Nullable
  @JsonInclude(Include.NON_NULL)
  private String url;
  @JsonInclude(Include.NON_NULL)
  @Nullable
  private String caption;
  @JsonInclude(Include.NON_NULL)
  @Nullable
  private String urlExpiry;
  @JsonInclude(Include.NON_NULL)
  @Nullable
  private String longitude;
  @JsonInclude(Include.NON_NULL)
  @Nullable
  private String latitude;
  @JsonInclude(Include.NON_NULL)
  @Nullable
  private Long ts;
  @Nullable
  @JsonInclude(Include.NON_NULL)
  private String reason;
  @Nullable
  @JsonInclude(Include.NON_NULL)
  private Long code;
  @JsonInclude(Include.NON_NULL)
  @Nullable
  private String whatsappMessageId;
  @JsonInclude(Include.NON_NULL)
  @Nullable
  private String type;
  @Nullable
  @JsonInclude(Include.NON_NULL)
  private String phone;
  @Nullable
  @JsonInclude(Include.NON_NULL)
  private String hsm;
  @Nullable
  @JsonInclude(Include.NON_NULL)
  private String fileName;

}
