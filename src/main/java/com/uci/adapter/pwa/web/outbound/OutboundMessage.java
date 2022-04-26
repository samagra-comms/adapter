package com.uci.adapter.pwa.web.outbound;

import lombok.*;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class OutboundMessage {
    private PwaMessage message;
    private String to;
    private String messageId;
}