package com.samagra.adapter.gs.whatsapp;

import com.google.inject.internal.cglib.core.$Signature;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class WAInboundFile {
    private String mime_type;
    private String signature;
    private String url;
    private String caption;
}
