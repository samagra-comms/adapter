package com.uci.adapter.netcore.whatsapp.outbound.interactive;

import java.util.ArrayList;

import jakarta.xml.bind.annotation.XmlRootElement;

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
