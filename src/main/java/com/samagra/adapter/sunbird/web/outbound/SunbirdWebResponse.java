package com.samagra.adapter.sunbird.web.outbound;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.samagra.adapter.netcore.whatsapp.outbound.SendMessageResponse;
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
public class SunbirdWebResponse {
   private String id;
   private String status;
   private String message;

}
