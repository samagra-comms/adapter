package com.uci.adapter.netcore.whatsapp;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

import com.uci.adapter.netcore.whatsapp.outbound.SingleMessage;
import com.uci.adapter.netcore.whatsapp.outbound.Text;
import com.uci.adapter.netcore.whatsapp.outbound.OutboundMessage;
import com.uci.adapter.netcore.whatsapp.outbound.SendMessageResponse;

import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;

public class NewNetcoreServiceTest {	 
	@Test
	public void sendTextTest() {
		String token = "eyJ0eXAiOiJKV1QiLCJhbGciOiJIUzUxMiJ9.eyJzdWIiOiJuZXRjb3Jlc2FsZXNleHAiLCJleHAiOjI0MjUxMDI1MjZ9.ljC4Tvgz031i6DsKr2ILgCJsc9C_hxdo2Kw8iZp9tsVcCaKbIOXaFoXmpU7Yo7ob4P6fBtNtdNBQv_NSMq_Q8w";
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
