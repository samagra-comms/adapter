package com.uci.adapter.pwa.web.outbound;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import javax.xml.bind.annotation.XmlRootElement;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@XmlRootElement
public class PwaWebResponse {
   private String id;
   private String status;
   private String message;

}
