package com.samagra.adapter.sunbird.web;

import com.samagra.adapter.sunbird.web.outbound.OutboundMessage;
import com.samagra.adapter.sunbird.web.outbound.SunbirdMessage;
import com.samagra.adapter.sunbird.web.outbound.SunbirdWebResponse;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import okhttp3.OkHttpClient;
import okhttp3.Response;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;


import java.io.IOException;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;


@Slf4j
@ExtendWith(MockitoExtension.class)
public class SunbirdWebServiceTest {

    @Mock
    private OkHttpClient client;

    @SneakyThrows
    @BeforeEach
    public void init() {

    }

    @Test
    public void sendTextTest() throws IOException {

        SunbirdMessage sunbirdMessage = SunbirdMessage.builder().title("Test").build();
        SunbirdMessage[] messages = {sunbirdMessage};
        String token = "token";
        SunbirdCredentials sc = SunbirdCredentials.builder().build();
        sc.setToken(token);
        String url ="https://waapi.pepipost.com/api/v2/";

        SunbirdWebService sws = new SunbirdWebService(sc);
        OutboundMessage outboundMessage = OutboundMessage.builder().message(messages).build();
        okhttp3.Call call = Mockito.mock(okhttp3.Call.class);
        okhttp3.ResponseBody body = Mockito.mock(okhttp3.ResponseBody.class);
        Mockito.when(body.string()).thenReturn("{\"id\":\"123233\",\"status\":\"success\",\"message\":\"Request received successfully.\"}");
        Response okkhttp3response=Mockito.mock(Response.class);
        Mockito.when(okkhttp3response.body()).thenReturn(body);
        Mockito.when(call.execute()).thenReturn(okkhttp3response);
        Mockito.when(client.newCall(Mockito.any())).thenReturn(call);
        ReflectionTestUtils.setField(sws,"client",client);

        SunbirdWebResponse response = sws.sendText(url, outboundMessage);
        assertNotNull(response);
        assertEquals(response.getStatus(), "success");
        assertEquals(response.getMessage(), "Request received successfully.");
        assertNotNull(response.getId());

    }

}
