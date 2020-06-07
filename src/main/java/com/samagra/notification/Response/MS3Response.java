package com.samagra.notification.Response;

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
public class MS3Response {
  private String currentIndex;
  private String nextMessage;
  private String currentResponseState;   //xml string
  private short isPreviousInputCorrect;
}
