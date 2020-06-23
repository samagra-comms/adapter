package com.samagra.message.TypeInterImp;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.NoArgsConstructor;

@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Location implements TypeInterface {
  public Double longitude;
  public Double latitude;
}
