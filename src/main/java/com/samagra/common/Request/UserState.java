package com.samagra.common.Request;

import java.util.HashMap;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Builder
@NoArgsConstructor
@AllArgsConstructor
@Setter
@XmlRootElement(name = "userstate")
public class UserState {
  @XmlElement(name = "phoneno")
  private String phoneno;
  @XmlElement(name = "questions")
  private HashMap<String, String> questions;
}
