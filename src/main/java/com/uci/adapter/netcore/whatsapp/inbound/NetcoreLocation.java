package com.uci.adapter.netcore.whatsapp.inbound;

import org.springframework.lang.Nullable;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class NetcoreLocation {
    private double latitude;
    private double longitude;
    
    @Nullable
    private String address;
    
    @Nullable
    private String url;
    
    @Nullable
    private String name;
}
