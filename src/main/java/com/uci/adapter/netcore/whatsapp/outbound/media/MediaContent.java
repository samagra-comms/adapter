package com.uci.adapter.netcore.whatsapp.outbound.media;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Builder
public class MediaContent {
	private Attachment[] attachments;
}
