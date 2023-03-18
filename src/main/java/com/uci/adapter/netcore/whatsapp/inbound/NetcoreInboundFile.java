package com.uci.adapter.netcore.whatsapp.inbound;

import com.fasterxml.jackson.annotation.JsonAlias;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class NetcoreInboundFile {
	@JsonAlias({"mime_type"})
    private String mimeType;
    private String sha256;
    private String id;
}
