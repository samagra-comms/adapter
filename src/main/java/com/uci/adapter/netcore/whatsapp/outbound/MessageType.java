package com.uci.adapter.netcore.whatsapp.outbound;

import com.uci.adapter.netcore.whatsapp.outbound.media.AttachmentType;

public enum MessageType {
	TEXT("text"),
	INTERACTIVE("interactive"),
	MEDIA("media");
	

    private String name;

    MessageType(String messageType) {
        name=messageType;
    }

    public String toString(){
        return name;
    }
}
