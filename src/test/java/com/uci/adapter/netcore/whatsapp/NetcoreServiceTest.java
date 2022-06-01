package com.uci.adapter.netcore.whatsapp;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.anyString;

import com.uci.utils.cache.service.RedisCacheService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.uci.adapter.netcore.whatsapp.outbound.SingleMessage;
import com.uci.adapter.netcore.whatsapp.outbound.Text;
import com.uci.adapter.netcore.whatsapp.outbound.OutboundMessage;
import com.uci.adapter.netcore.whatsapp.outbound.SendMessageResponse;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.springframework.data.redis.core.RedisTemplate;

public class NetcoreServiceTest {

	@Mock
	NWCredentials credentials;
	NewNetcoreService newNetcoreService;

	@BeforeEach
	void init(){
		String token = "token";
		credentials = NWCredentials.builder().build();
		credentials.setToken(token);
//
//		NewNetcoreService nns = new NewNetcoreService(credentials);
		newNetcoreService = Mockito.spy(new NewNetcoreService(credentials));

		/* Mock Data */
		SendMessageResponse obj = new SendMessageResponse();
		obj.setStatus("success");
		obj.setMessage("Success!");
		Mockito.doReturn(obj).when(newNetcoreService).sendText(new OutboundMessage());

	}

//	@Test
//	public void sendTextTest() {
//		// Use your netcore token to send message to netcore
//		Text text = Text.builder().content("Hello").previewURL("false").build();
//        Text[] texts = {text};
//
//		SingleMessage msg = SingleMessage
//                .builder()
//                .from("461089f9-1000-4211-b182-c7f0291f3d45")
//                .to("917597185708")
//                .recipientType("individual")
//                .messageType("text")
//                .header("custom_data")
//                .text(texts)
//                .build();
//        SingleMessage[] messages = {msg};
//
//		OutboundMessage message = OutboundMessage.builder().message(messages).build();
//
//		/* Send test message to number from UCI Netcore chatbot */
//		SendMessageResponse response = newNetcoreService.sendText(message);
//
////		System.out.println(response);
//		assertNotNull(response);
//		assertEquals(response.getStatus(), "success");
//		assertNotNull(response.getMessage());
////		assertNotNull(response.getData());
////        assertNotNull(response.getData().getIdentifier());
//	}
}
