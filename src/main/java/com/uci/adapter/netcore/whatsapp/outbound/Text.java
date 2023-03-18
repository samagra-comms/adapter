package com.uci.adapter.netcore.whatsapp.outbound;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

import jakarta.xml.bind.annotation.XmlRootElement;

@Getter
@Setter
@XmlRootElement
@Builder
public class Text {

    @JsonProperty("preview_url")
    @JsonAlias({"preview_url"})
    private String previewURL;

    @JsonProperty("content")
    @JsonAlias({"content"})
    private String content;
}
