package com.uci.adapter.gs.whatsapp;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.uci.dao.models.XMessageDAO;
import com.uci.dao.repository.XMessageRepository;
import com.uci.utils.BotService;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import messagerosa.core.model.XMessage;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import reactor.core.publisher.Mono;

import jakarta.xml.bind.JAXBException;

import java.util.ArrayList;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.notNull;

@Slf4j
@ExtendWith(MockitoExtension.class)
class GupShupWhatsappAdapterTest{

    GupShupWhatsappAdapter adapter;
    ObjectMapper objectMapper;
    String simplePayload, readPayload, sentPayload, deliveredPayload;

    @Mock
    BotService botService;

    @Mock
    XMessageRepository xMessageRepo;

    @Mock
    XMessageDAO xMessageDAO;

    @SneakyThrows
    @BeforeEach
    public void init() {
//        when(botService.getCurrentAdapter(any())).thenReturn(Mono.just("A"));
//        when(botService.getCampaignFromStartingMessage(any())).thenReturn(Mono.just("test"));

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
//        when(xMessageRepo.findAllByUserIdOrderByTimestamp((String) notNull())).thenReturn(xMessageDAOArrayList);

        GSWhatsAppMessage message = objectMapper.readValue(simplePayload, GSWhatsAppMessage.class);
        Mono<XMessage> xMessage = adapter.convertMessageToXMsg(message);

        xMessage.log().subscribe(System.out::println,
                (e) -> System.err.println("---------------Exception occured in converting Message to XMessage-------------: " + e),
                () -> System.out.println("------------Convert Message to XMessage completed-----------"));
    }

    @Test
    public void readPayloadParsing() throws JsonProcessingException, JAXBException {

        GSWhatsAppMessage message = objectMapper.readValue(readPayload, GSWhatsAppMessage.class);
        Mono<XMessage> xMessage = adapter.convertMessageToXMsg(message);

        xMessage.log().subscribe(System.out::println,
                (e) -> System.err.println("---------------Exception occured in converting Message to XMessage-------------: " + e),
                () -> System.out.println("------------Convert Message to XMessage completed-----------"));
    }

    @Test
    public void sentPayloadParsing() throws JsonProcessingException, JAXBException {

        GSWhatsAppMessage message = objectMapper.readValue(sentPayload, GSWhatsAppMessage.class);
        Mono<XMessage> xMessage = adapter.convertMessageToXMsg(message);

        xMessage.log().subscribe(System.out::println,
                (e) -> System.err.println("---------------Exception occured in converting Message to XMessage-------------: " + e),
                () -> System.out.println("------------Convert Message to XMessage completed-----------"));
    }

    @Test
    public void deliveredPayloadParsing() throws JsonProcessingException, JAXBException {

        GSWhatsAppMessage message = objectMapper.readValue(deliveredPayload, GSWhatsAppMessage.class);
        Mono<XMessage> xMessage = adapter.convertMessageToXMsg(message);

        xMessage.log().subscribe(System.out::println,
                (e) -> System.err.println("---------------Exception occured in converting Message to XMessage-------------: " + e),
                () -> System.out.println("------------Convert Message to XMessage completed-----------"));
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
