package com.uci.adapter.gs.whatsapp.outbound;

public enum MethodType {
	SIMPLEMESSAGE("SendMessage"),
	MEDIAMESSAGE("SendMediaMessage"),
	OPTIN("OPT_IN");
	
	private String name;

    MethodType(String methodType) {
        name=methodType;
    }

    public String toString(){
        return name;
    }
}	
