package com.uci.adapter.sunbird.web.outbound;

import lombok.*;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class OutboundMessage {
    private SunbirdMessage message;
}