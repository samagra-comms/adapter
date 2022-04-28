package com.uci.adapter.netcore.whatsapp;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import org.junit.jupiter.api.Test;

import com.uci.adapter.netcore.whatsapp.outbound.SingleMessage;
import com.uci.adapter.netcore.whatsapp.outbound.Text;
import com.uci.adapter.netcore.whatsapp.outbound.OutboundMessage;
import com.uci.adapter.netcore.whatsapp.outbound.SendMessageResponse;

public class NetcoreServiceTest {
	@Test
	public void sendTextTest() {
		// Use your netcore token to send message to netcore
		String token = "token";
		NWCredentials credentials = NWCredentials.builder().build();
		credentials.setToken(token);
		
		NewNetcoreService nns = new NewNetcoreService(credentials);
		
		Text text = Text.builder().content("Hello").previewURL("false").build();
        Text[] texts = {text};
		
		SingleMessage msg = SingleMessage
                .builder()
                .from("461089f9-1000-4211-b182-c7f0291f3d45")
                .to("917597185708")
                .recipientType("individual")
                .messageType("text")
                .header("custom_data")
                .text(texts)
                .build();
        SingleMessage[] messages = {msg};
        
		OutboundMessage message = OutboundMessage.builder().message(messages).build();
		
		/* Send test message to number from UCI Netcore chatbot */
		SendMessageResponse response = nns.sendText(message);
		
//		System.out.println(response);
		assertNotNull(response);
		assertEquals(response.getStatus(), "success");
		assertNotNull(response.getMessage());
		assertNotNull(response.getData());
        assertNotNull(response.getData().getIdentifier());
	}
}
