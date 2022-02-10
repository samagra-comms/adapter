package com.uci.adapter.netcore.whatsapp.outbound;

import java.util.ArrayList;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Builder
public class ListSection {
	private String title;
	
	private ArrayList<ListSectionRow> rows;
}
