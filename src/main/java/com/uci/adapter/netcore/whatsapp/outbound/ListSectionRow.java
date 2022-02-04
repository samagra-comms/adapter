package com.uci.adapter.netcore.whatsapp.outbound;

import java.util.ArrayList;

import javax.annotation.Nullable;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Builder
public class ListSectionRow {
	private String id;
	private String title;
	private String description;
}
