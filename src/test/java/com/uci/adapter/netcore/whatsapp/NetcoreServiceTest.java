package com.uci.adapter.netcore.whatsapp;

import java.io.IOException;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.uci.adapter.AdapterTestConfiguration;
import com.uci.adapter.netcore.whatsapp.outbound.OutboundMessage;
import com.uci.adapter.netcore.whatsapp.outbound.SendMessageResponse;
import com.uci.adapter.netcore.whatsapp.outbound.SingleMessage;
import com.uci.adapter.netcore.whatsapp.outbound.Text;
import okhttp3.Call;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.ResponseBody;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;

import org.springframework.boot.test.context.SpringBootTest;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

@SpringBootTest(classes = AdapterTestConfiguration.class)
public class NetcoreServiceTest {

	@Mock
	private OkHttpClient client;

	@InjectMocks
	private NewNetcoreService netcoreService;

	@Test
	public void sendTextTest() throws IOException {
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

		String mockResponseBody =
				"{"
				+ "\"status\":\"success\","
				+ "\"message\":\"Message sent successfully\","
				+ "\"data\":{"
				+ "\"identifier\":\"1234\""
				+ "},"
				+ "\"error\":null"
				+ "}";

		try (Response mockResponse = Mockito.mock(Response.class)) {
			Call callMock = Mockito.mock(Call.class);
			Mockito.when(mockResponse.body()).thenReturn(ResponseBody.create(mockResponseBody.getBytes(), MediaType.parse("application/json")));
			Mockito.when(callMock.execute()).thenReturn(mockResponse);
			Mockito.when(client.newCall(Mockito.any(Request.class))).thenReturn(callMock);

			/* Send test message to number from UCI Netcore chatbot */
			SendMessageResponse response = netcoreService.sendText(message);
			Mockito.verify(callMock).execute();
			assertNotNull(response);
			assertEquals(new ObjectMapper().writeValueAsString(response), mockResponseBody);
			assertEquals(response.getStatus(), "success");
			assertNotNull(response.getMessage());
			assertNotNull(response.getData());
			assertNotNull(response.getData().getIdentifier());
		}
	}
}
