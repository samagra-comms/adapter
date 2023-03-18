package com.uci.adapter.netcore.whatsapp.inbound;

import com.fasterxml.jackson.annotation.JsonAlias;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class InteractiveContent {
	private String type;
	
	@JsonAlias({"button_reply"})
	private QuickReplyButton buttonReply;
	
	@JsonAlias({"list_reply"})
	private ListReply listReply;
}
