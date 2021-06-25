package com.samagra.adapter.gs.whatsapp;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.uci.utils.BotService;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import messagerosa.core.model.XMessage;
import messagerosa.dao.XMessageDAO;
import messagerosa.dao.XMessageRepo;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import javax.xml.bind.JAXBException;

import java.util.ArrayList;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.notNull;
import static org.mockito.Mockito.when;

@Slf4j
@ExtendWith(MockitoExtension.class)
class GupShupWhatsappAdapterTest{

    GupShupWhatsappAdapter adapter;
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
        simplePayload = "{\"waNumber\":\"919311415686\",\"mobile\":\"919415787824\",\"replyId\":null,\"messageId\":null,\"timestamp\":1616952476000,\"name\":\"chaks\",\"version\":0,\"type\":\"text\",\"text\":\"*\",\"image\":null,\"document\":null,\"voice\":null,\"audio\":null,\"video\":null,\"location\":null,\"response\":null,\"extra\":null,\"app\":null}";
        readPayload = "{\"waNumber\":null,\"mobile\":null,\"replyId\":null,\"messageId\":null,\"timestamp\":null,\"name\":null,\"version\":0,\"type\":null,\"text\":null,\"image\":null,\"document\":null,\"voice\":null,\"audio\":null,\"video\":null,\"location\":null,\"response\":\"[{\\\"srcAddr\\\":\\\"SDTEXT\\\",\\\"extra\\\":\\\"Samagra\\\",\\\"channel\\\":\\\"WHATSAPP\\\",\\\"externalId\\\":\\\"4340925846643462155-31668054994359383\\\",\\\"cause\\\":\\\"READ\\\",\\\"errorCode\\\":\\\"026\\\",\\\"destAddr\\\":\\\"919415787824\\\",\\\"eventType\\\":\\\"READ\\\",\\\"eventTs\\\":1616990315000}]\",\"extra\":null,\"app\":null}";
        sentPayload = "{\"waNumber\":null,\"mobile\":null,\"replyId\":null,\"messageId\":null,\"timestamp\":null,\"name\":null,\"version\":0,\"type\":null,\"text\":null,\"image\":null,\"document\":null,\"voice\":null,\"audio\":null,\"video\":null,\"location\":null,\"response\":\"[{\\\"srcAddr\\\":\\\"SDTEXT\\\",\\\"extra\\\":\\\"Samagra\\\",\\\"channel\\\":\\\"WHATSAPP\\\",\\\"externalId\\\":\\\"4340925846643462155-31668054994359383\\\",\\\"cause\\\":\\\"SENT\\\",\\\"errorCode\\\":\\\"025\\\",\\\"destAddr\\\":\\\"919415787824\\\",\\\"eventType\\\":\\\"SENT\\\",\\\"eventTs\\\":1616990314000}]\",\"extra\":null,\"app\":null}";
        deliveredPayload = "{\"waNumber\":null,\"mobile\":null,\"replyId\":null,\"messageId\":null,\"timestamp\":null,\"name\":null,\"version\":0,\"type\":null,\"text\":null,\"image\":null,\"document\":null,\"voice\":null,\"audio\":null,\"video\":null,\"location\":null,\"response\":\"[{\\\"srcAddr\\\":\\\"SDTEXT\\\",\\\"extra\\\":\\\"Samagra\\\",\\\"channel\\\":\\\"WHATSAPP\\\",\\\"externalId\\\":\\\"4340928795421794315-368294223055997520\\\",\\\"cause\\\":\\\"SUCCESS\\\",\\\"errorCode\\\":\\\"000\\\",\\\"destAddr\\\":\\\"919415787824\\\",\\\"eventType\\\":\\\"DELIVERED\\\",\\\"eventTs\\\":1616990666000}]\",\"extra\":null,\"app\":null}";

        //TODO: Add a payload for Files, Videos and Location.

        adapter = GupShupWhatsappAdapter
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

        GSWhatsAppMessage message = objectMapper.readValue(simplePayload, GSWhatsAppMessage.class);
        XMessage xMessage = adapter.convertMessageToXMsg(message);
        assertEquals("test", xMessage.getApp());
        assertEquals("9415787824", xMessage.getFrom().getUserID());
        assertEquals("A", xMessage.getAdapterId());
        assertEquals("WhatsApp", xMessage.getChannelURI());
        assertEquals("gupshup", xMessage.getProviderURI());
        assertEquals("REPLIED", xMessage.getMessageState().toString());
    }

    @Test
    public void readPayloadParsing() throws JsonProcessingException, JAXBException {

        GSWhatsAppMessage message = objectMapper.readValue(readPayload, GSWhatsAppMessage.class);
        XMessage xMessage = adapter.convertMessageToXMsg(message);
        assertEquals("test", xMessage.getApp());
        assertEquals("9415787824", xMessage.getFrom().getUserID());
        assertEquals("A", xMessage.getAdapterId());
        assertEquals("WhatsApp", xMessage.getChannelURI());
        assertEquals("gupshup", xMessage.getProviderURI());
        assertEquals("READ", xMessage.getMessageState().toString());
    }

    @Test
    public void sentPayloadParsing() throws JsonProcessingException, JAXBException {

        GSWhatsAppMessage message = objectMapper.readValue(sentPayload, GSWhatsAppMessage.class);
        XMessage xMessage = adapter.convertMessageToXMsg(message);
        assertEquals("test", xMessage.getApp());
        assertEquals("9415787824", xMessage.getFrom().getUserID());
        assertEquals("A", xMessage.getAdapterId());
        assertEquals("WhatsApp", xMessage.getChannelURI());
        assertEquals("gupshup", xMessage.getProviderURI());
        assertEquals("SENT", xMessage.getMessageState().toString());
    }

    @Test
    public void deliveredPayloadParsing() throws JsonProcessingException, JAXBException {

        GSWhatsAppMessage message = objectMapper.readValue(deliveredPayload, GSWhatsAppMessage.class);
        XMessage xMessage = adapter.convertMessageToXMsg(message);
        assertEquals("test", xMessage.getApp());
        assertEquals("9415787824", xMessage.getFrom().getUserID());
        assertEquals("A", xMessage.getAdapterId());
        assertEquals("WhatsApp", xMessage.getChannelURI());
        assertEquals("gupshup", xMessage.getProviderURI());
        assertEquals("DELIVERED",xMessage.getMessageState().toString());
    }

//    @Test
//    public void optedInPayloadParsing() throws JsonProcessingException, JAXBException {
//        String payload = "";
//        GSWhatsAppMessage message = new ObjectMapper().readValue(payload, GSWhatsAppMessage.class);
//        XMessage xMessage = adapter.convertMessageToXMsg(message);
//        assertEquals(xMessage.getMessageState(), XMessage.MessageState.OPTED_IN);
//    }

    @AfterAll
    static void teardown() {
        System.out.println("Teardown 43");
    }

}
