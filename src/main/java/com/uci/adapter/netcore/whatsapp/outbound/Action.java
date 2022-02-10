package com.uci.adapter.netcore.whatsapp.outbound;

import java.util.ArrayList;

import javax.xml.bind.annotation.XmlRootElement;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.sun.istack.Nullable;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Builder
public class Action {
	
	@Nullable
	private ArrayList<QuickReplyButton> buttons;
	
	@Nullable
	private String button;
	
	private ListSection[] sections;
}
