package com.uci.adapter.netcore.whatsapp.outbound;

import lombok.*;
import org.json.JSONObject;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class OutboundMessage {
    private SingleMessage[] message;
}
