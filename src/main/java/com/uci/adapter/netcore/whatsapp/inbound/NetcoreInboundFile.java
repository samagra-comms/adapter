package com.uci.adapter.netcore.whatsapp.inbound;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class NetcoreInboundFile {
    private String mime_type;
    private String signature;
    private String url;
    private String caption;
}
