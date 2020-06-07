package com.samagra.common.Request;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Builder
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class MS3Request {
  private String previousPath;
  private String currentAnswer;
  private String instanceXMlPrevious;
  private String botFormName;
}
