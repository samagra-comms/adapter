package com.uci.adapter.netcore.whatsapp.outbound.interactive.list;

import java.util.ArrayList;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Builder
public class Section {
	private String title;
	
	private ArrayList<SectionRow> rows;
}
