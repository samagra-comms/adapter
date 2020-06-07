package com.samagra.common.Request;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class FormRequest {
  private String formName;
  private Long formID;
  private String welcomeMessage;
  private String wrongDefaultMessage;
}
