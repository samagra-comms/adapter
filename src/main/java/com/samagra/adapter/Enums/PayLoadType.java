package com.samagra.adapter.Enums;

import java.util.HashMap;
import java.util.Map;
import javax.xml.bind.annotation.XmlEnum;
import lombok.AllArgsConstructor;
import lombok.Getter;

@AllArgsConstructor
@XmlEnum
@Getter
public enum PayLoadType {
  text("text"),
  image("image"),
  audio("audio"),
  video("video"),
  file("file"),
  location("location"),
  sandboxStart("sandbox-start"),
  optedIn("opted-in"),
  optedOut("opted-out"),
  enqueued("enqueued"),
  failed("failed"),
  sent("sent"),
  delivered("delivered"),
  read("read");

  private static final Map<String, PayLoadType> map = new HashMap<>(values().length, 1);

  static {
    for (PayLoadType c : values())
      map.put(c.category, c);
  }

  private final String category;


  public static PayLoadType of(String name) {
    PayLoadType result = map.get(name);
    if (result == null) {
      throw new IllegalArgumentException("Invalid category name: " + name);
    }
    return result;
  }
}
