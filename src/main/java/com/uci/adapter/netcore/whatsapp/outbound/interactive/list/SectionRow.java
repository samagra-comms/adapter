package com.uci.adapter.netcore.whatsapp.outbound.interactive.list;

import jakarta.annotation.Nullable;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Builder
public class SectionRow {
	private String id;
	private String title;
	
//	@Nullable
//	private String description;
}
