package com.uci.adapter.netcore.whatsapp.outbound.media;

public enum AttachmentType {
	IMAGE("image"),
	AUDIO("audio"),
	VIDEO("video"),
	DOCUMENT("document");

    private String name;

    AttachmentType(String attachmentType) {
        name=attachmentType;
    }

    public String toString(){
        return name;
    }
}
