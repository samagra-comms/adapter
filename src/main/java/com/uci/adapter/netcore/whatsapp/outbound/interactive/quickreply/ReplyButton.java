package com.uci.adapter.netcore.whatsapp.outbound.interactive.quickreply;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Builder
public class ReplyButton {
	private String id;
	private String title;
}
