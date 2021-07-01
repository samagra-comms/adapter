package com.samagra.adapter.Enums;

import java.util.HashMap;
import java.util.Map;
import javax.xml.bind.annotation.XmlEnum;
import lombok.AllArgsConstructor;

@AllArgsConstructor
@XmlEnum

public enum MessageType {
  message("message"),
  userevent("user-event"),
  messageevent("message-event");

  private static final Map<String, MessageType> map = new HashMap<>(values().length, 1);

  static {
    for (MessageType c : values())
      map.put(c.messageType, c);
  }

  private final String messageType;


  public static MessageType of(String name) {
    MessageType result = map.get(name);
    if (result == null) {
      throw new IllegalArgumentException("Invalid category name: " + name);
    }
    return result;
  }
}
