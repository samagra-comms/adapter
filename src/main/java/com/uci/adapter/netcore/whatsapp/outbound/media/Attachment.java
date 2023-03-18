package com.uci.adapter.netcore.whatsapp.outbound.media;

import jakarta.annotation.Nullable;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Builder
public class Attachment {
	private String attachment_url;
	
	private String attachment_type;
	
	@Nullable
	private String caption;
}
