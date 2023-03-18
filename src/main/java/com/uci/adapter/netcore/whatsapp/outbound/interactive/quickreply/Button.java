package com.uci.adapter.netcore.whatsapp.outbound.interactive.quickreply;

import jakarta.xml.bind.annotation.XmlRootElement;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Builder
public class Button {
	private String type;
	
	private ReplyButton reply;
}
