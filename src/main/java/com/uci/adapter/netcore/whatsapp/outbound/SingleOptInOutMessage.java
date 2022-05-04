package com.uci.adapter.netcore.whatsapp.outbound;

import org.springframework.lang.Nullable;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonProperty;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

@Setter
@Getter
@Builder
public class SingleOptInOutMessage {
	@JsonProperty("recipient")
	@JsonAlias({"recipient"})
	private String to;
	
	@JsonProperty("source")
	@JsonAlias({"source"})
	private String from;
	
	@Nullable
	@JsonProperty("user_agent")
	@JsonAlias({"user_agent"})
	private String userAgent;
	
	@Nullable
	private String ip;
}
