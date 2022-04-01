package com.uci.adapter.netcore.whatsapp.outbound;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonProperty;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

@Setter
@Getter
@Builder
public class OutboundOptInOutMessage {
	private String type;
	
    @JsonProperty("recipients")
	@JsonAlias({"recipients"})
	private SingleOptInOutMessage[] recipients;
}
