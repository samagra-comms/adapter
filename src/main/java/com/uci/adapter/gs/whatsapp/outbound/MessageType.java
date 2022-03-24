package com.uci.adapter.gs.whatsapp.outbound;

public enum MessageType {
	TEXT("TEXT"),
	HSM("HSM"),
	IMAGE("IMAGE"),
	AUDIO("AUDIO"),
	VIDEO("VIDEO"),
	DOCUMENT("DOCUMENT");
	
	private String name;

    MessageType(String messageType) {
        name=messageType;
    }

    public String toString(){
        return name;
    }
}
