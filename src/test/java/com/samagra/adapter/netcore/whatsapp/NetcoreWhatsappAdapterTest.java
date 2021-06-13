package com.samagra.adapter.netcore.whatsapp;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.samagra.adapter.netcore.whatsapp.inbound.NetcoreWhatsAppMessage;
import com.samagra.user.BotService;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import messagerosa.core.model.XMessage;
import messagerosa.dao.XMessageDAO;
import messagerosa.dao.XMessageRepo;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import javax.xml.bind.JAXBException;
import java.util.ArrayList;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.notNull;
import static org.mockito.Mockito.when;

@Slf4j
@ExtendWith(MockitoExtension.class)
class NetcoreWhatsappAdapterTest {

    NetcoreWhatsappAdapter adapter;
    ObjectMapper objectMapper;
    String simplePayload, readPayload, sentPayload, deliveredPayload;

    @Mock
    BotService botService;

    @Mock
    XMessageRepo xMessageRepo;

    @Mock
    XMessageDAO xMessageDAO;

    @SneakyThrows
    @BeforeEach
    public void init() {
        when(botService.getCurrentAdapter(any())).thenReturn("A");
        when(botService.getCampaignFromStartingMessage(any())).thenReturn("test");

        objectMapper = new ObjectMapper();
        simplePayload = "{\"message_id\":\"ABEGkZlgQyWAAgo-sDVSUOa9jH0z\",\"from\":\"919415787824\",\"received_at\":\"1567090835\",\"context\": {\"ncmessage_id\":null,\"message_id\":null},\"message_type\":\"TEXT\",\"text_type\":{\"text\":\"test\"}}";
        readPayload = "{\"ncmessage_id\":\"fa9d647a-c8d7-423e-bd27-7d2ca2875d12\",\"recipient\":\"919415787824\",\"status\":\"read\",\"status_remark\":\"\",\"received_at\":\"2019-05-16 15:36:58\",\"source\":\"fa9d647a-c8d7-423e-bd27-7d2ca2875dc1\"}";
        sentPayload = "{\"ncmessage_id\":\"fa9d647a-c8d7-423e-bd27-7d2ca2875d12\",\"recipient\":\"919415787824\",\"status\":\"sent\",\"status_remark\":\"\",\"received_at\":\"2019-05-16 15:36:58\",\"source\":\"fa9d647a-c8d7-423e-bd27-7d2ca2875dc1\"}";
        deliveredPayload = "{\"ncmessage_id\":\"fa9d647a-c8d7-423e-bd27-7d2ca2875d12\",\"recipient\":\"919415787824\",\"status\":\"delivered\",\"status_remark\":\"\",\"received_at\":\"2019-05-16 15:36:58\",\"source\":\"fa9d647a-c8d7-423e-bd27-7d2ca2875dc1\"}";;

        //TODO: Add a payload for Files, Videos and Location.

        adapter = NetcoreWhatsappAdapter
                .builder()
                .botservice(botService)
                .xmsgRepo(xMessageRepo)
                .build();
    }

    @Test
    public void simplePayloadParsing() throws JsonProcessingException, JAXBException {
        ArrayList<XMessageDAO> xMessageDAOArrayList = new ArrayList<>();
        xMessageDAOArrayList.add(xMessageDAO);
        when(xMessageRepo.findAllByUserIdOrderByTimestamp((String) notNull())).thenReturn(xMessageDAOArrayList);

        NetcoreWhatsAppMessage message = objectMapper.readValue(simplePayload, NetcoreWhatsAppMessage.class);
        XMessage xMessage = adapter.convertMessageToXMsg(message);
        assertEquals("test", xMessage.getApp());
        assertEquals("9415787824", xMessage.getFrom().getUserID());
        assertEquals("A", xMessage.getAdapterId());
        assertEquals("WhatsApp", xMessage.getChannelURI());
        assertEquals("Netcore", xMessage.getProviderURI());
        assertEquals("REPLIED", xMessage.getMessageState().toString());
    }

    @Test
    public void readPayloadParsing() throws JsonProcessingException, JAXBException {

        NetcoreWhatsAppMessage message = objectMapper.readValue(readPayload, NetcoreWhatsAppMessage.class);
        XMessage xMessage = adapter.convertMessageToXMsg(message);
        assertEquals("test", xMessage.getApp());
        assertEquals("9415787824", xMessage.getFrom().getUserID());
        assertEquals("A", xMessage.getAdapterId());
        assertEquals("WhatsApp", xMessage.getChannelURI());
        assertEquals("Netcore", xMessage.getProviderURI());
        assertEquals("READ", xMessage.getMessageState().toString());
    }

    @Test
    public void sentPayloadParsing() throws JsonProcessingException, JAXBException {

        NetcoreWhatsAppMessage message = objectMapper.readValue(sentPayload, NetcoreWhatsAppMessage.class);
        XMessage xMessage = adapter.convertMessageToXMsg(message);
        assertEquals("test", xMessage.getApp());
        assertEquals("9415787824", xMessage.getFrom().getUserID());
        assertEquals("A", xMessage.getAdapterId());
        assertEquals("WhatsApp", xMessage.getChannelURI());
        assertEquals("Netcore", xMessage.getProviderURI());
        assertEquals("SENT", xMessage.getMessageState().toString());
    }

    @Test
    public void deliveredPayloadParsing() throws JsonProcessingException, JAXBException {

        NetcoreWhatsAppMessage message = objectMapper.readValue(deliveredPayload, NetcoreWhatsAppMessage.class);
        XMessage xMessage = adapter.convertMessageToXMsg(message);
        assertEquals("test", xMessage.getApp());
        assertEquals("9415787824", xMessage.getFrom().getUserID());
        assertEquals("A", xMessage.getAdapterId());
        assertEquals("WhatsApp", xMessage.getChannelURI());
        assertEquals("Netcore", xMessage.getProviderURI());
        assertEquals("DELIVERED",xMessage.getMessageState().toString());
    }

    @AfterAll
    static void teardown() {
        System.out.println("Teardown 43");
    }

}
