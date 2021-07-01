package com.uci.adapter.netcore.whatsapp;

import com.uci.adapter.netcore.whatsapp.outbound.OutboundMessage;
import com.uci.adapter.netcore.whatsapp.outbound.SendMessageResponse;
import com.uci.adapter.netcore.whatsapp.outbound.SingleMessage;
import com.uci.adapter.netcore.whatsapp.outbound.Text;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.notNull;

@Slf4j
@ExtendWith(MockitoExtension.class)
class NetcoreServiceTest {

    @SneakyThrows
    @BeforeEach
    public void init() {

    }

    @Test
    public void manageUsersTest() {

    }

    @Test
    public void sendTextTest() {

        Text text = Text.builder().content("Hello").previewURL("false").build();
        Text[] texts = {text};

        SingleMessage msg = SingleMessage
                .builder()
                .from("461089f9-1000-4211-b182-c7f0291f3d45")
                .to("919415787824")
                .recipientType("individual")
                .messageType("text")
                .header("custom_data")
                .text(texts)
                .build();
        SingleMessage[] messages = {msg};

        String token = "eyJ0eXAiOiJKV1QiLCJhbGciOiJIUzUxMiJ9.eyJzdWIiOiJuZXRjb3Jlc2FsZXNleHAiLCJleHAiOjI0MjUxMDI1MjZ9.ljC4Tvgz031i6DsKr2ILgCJsc9C_hxdo2Kw8iZp9tsVcCaKbIOXaFoXmpU7Yo7ob4P6fBtNtdNBQv_NSMq_Q8w";
        NWCredentials nc = NWCredentials.builder().build();
        nc.setToken(token);
        NetcoreService ns = new NetcoreService(nc);

        OutboundMessage outboundMessage = OutboundMessage.builder().message(messages).build();
        SendMessageResponse response = ns.sendText(outboundMessage);

        assertNotNull(response);
        assertEquals(response.getStatus(), "success");
        assertEquals(response.getMessage(), "Request received successfully.");
        assertNotNull(response.getData());
        assertNotNull(response.getData().getIdentifier());

    }

    @AfterAll
    static void teardown() {
        System.out.println("Teardown");
    }

}
