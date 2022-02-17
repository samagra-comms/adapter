package com.uci.adapter.netcore.whatsapp.outbound.interactive;

import java.util.ArrayList;

import javax.xml.bind.annotation.XmlRootElement;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.sun.istack.Nullable;
import com.uci.adapter.netcore.whatsapp.outbound.interactive.list.Section;
import com.uci.adapter.netcore.whatsapp.outbound.interactive.quickreply.Button;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Builder
public class Action {
	
	@Nullable
	private ArrayList<Button> buttons;
	
	@Nullable
	private String button;
	
	private Section[] sections;
}
