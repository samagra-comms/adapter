package com.samagra.adapter.sunbird.web.outbound;

import com.samagra.adapter.netcore.whatsapp.outbound.SingleMessage;
import lombok.*;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class OutboundMessage {
    private SunbirdMessage[] message;
}