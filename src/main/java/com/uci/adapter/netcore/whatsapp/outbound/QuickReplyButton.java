package com.uci.adapter.netcore.whatsapp.outbound;

import java.util.ArrayList;

import javax.xml.bind.annotation.XmlRootElement;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Builder
public class QuickReplyButton {
	private String type;
	
	private ReplyButton reply;
}
