package com.samagra.adapter.netcore.whatsapp;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.samagra.adapter.netcore.whatsapp.inbound.NetcoreWhatsAppMessage;
import com.uci.utils.BotService;
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
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import javax.xml.bind.JAXBException;
import java.util.ArrayList;
import java.util.function.Consumer;

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
        when(botService.getCurrentAdapter(any())).thenReturn(Mono.just("A"));
//        when(botService.getCampaignFromStartingMessage(any())).thenReturn(Mono.just("test"));

        objectMapper = new ObjectMapper();
        simplePayload = "{\"waNumber\":null,\"mobile\":\"919415787824\",\"replyId\":null,\"messageId\":\"ABEGkXg3gzEAAgo-sBE0mjxmPR_L\",\"timestamp\":\"1624872561\",\"name\":null,\"version\":0,\"type\":\"TEXT\",\"text\":{\"text\":\"1\"},\"eventType\":null,\"context\":{\"ncmessage_id\":null,\"message_id\":null},\"statusRemark\":null,\"source\":null,\"image\":null,\"document\":null,\"voice\":null,\"audio\":null,\"video\":null,\"location\":null,\"response\":null,\"extra\":null,\"app\":null}";
        readPayload = "{\"waNumber\":null,\"mobile\":\"919415787824\",\"replyId\":null,\"messageId\":\"30da41c7-4c01-48df-b03c-3eecb8f686b4\",\"timestamp\":\"1624872929\",\"name\":null,\"version\":0,\"type\":null,\"text\":null,\"eventType\":\"read\",\"context\":null,\"statusRemark\":null,\"source\":\"461089f9-1000-4211-b182-c7f0291f3d45\",\"image\":null,\"document\":null,\"voice\":null,\"audio\":null,\"video\":null,\"location\":null,\"response\":null,\"extra\":null,\"app\":null}";
        sentPayload = "{\"waNumber\":null,\"mobile\":\"919415787824\",\"replyId\":null,\"messageId\":\"4ab8fa54-c7df-4c8c-98c6-2aa9e88d0503\",\"timestamp\":\"1624872902\",\"name\":null,\"version\":0,\"type\":null,\"text\":null,\"eventType\":\"sent\",\"context\":null,\"statusRemark\":null,\"source\":\"461089f9-1000-4211-b182-c7f0291f3d45\",\"image\":null,\"document\":null,\"voice\":null,\"audio\":null,\"video\":null,\"location\":null,\"response\":null,\"extra\":null,\"app\":null}";
        deliveredPayload = "{\"waNumber\":null,\"mobile\":\"919415787824\",\"replyId\":null,\"messageId\":\"4ab8fa54-c7df-4c8c-98c6-2aa9e88d0503\",\"timestamp\":\"1624872903\",\"name\":null,\"version\":0,\"type\":null,\"text\":null,\"eventType\":\"delivered\",\"context\":null,\"statusRemark\":null,\"source\":\"461089f9-1000-4211-b182-c7f0291f3d45\",\"image\":null,\"document\":null,\"voice\":null,\"audio\":null,\"video\":null,\"location\":null,\"response\":null,\"extra\":null,\"app\":null}";

        XMessageDAO xMessageDAO =  XMessageDAO.builder().app("test").build();

        when(xMessageRepo.findTopByUserIdAndMessageStateOrderByTimestampDesc(any(), any())).thenReturn(xMessageDAO);
        //TODO: Add a payload for Files, Videos and Location.

        adapter = NetcoreWhatsappAdapter
                .builder()
                .botservice(botService)
                .build();
    }

    @Test
    public void simplePayloadParsing() throws JsonProcessingException, JAXBException {
        ArrayList<XMessageDAO> xMessageDAOArrayList = new ArrayList<>();
        xMessageDAOArrayList.add(xMessageDAO);
        when(xMessageRepo.findAllByUserIdOrderByTimestamp((String) notNull())).thenReturn(xMessageDAOArrayList);

        NetcoreWhatsAppMessage message = objectMapper.readValue(simplePayload, NetcoreWhatsAppMessage.class);
        Mono<XMessage> xMessage = adapter.convertMessageToXMsg(message);

        StepVerifier.create(xMessage)
                .consumeNextWith(new Consumer<XMessage>() {
                    @Override
                    public void accept(XMessage xMessage) {
                        assertEquals("test", xMessage.getApp());
                        assertEquals("9415787824", xMessage.getFrom().getUserID());
                        assertEquals("A", xMessage.getAdapterId());
                        assertEquals("WhatsApp", xMessage.getChannelURI());
                        assertEquals("Netcore", xMessage.getProviderURI());
                        assertEquals("REPLIED", xMessage.getMessageState().toString());
                    }
                })
                .expectComplete()
                .verify();
    }

    @Test
    public void readPayloadParsing() throws JsonProcessingException, JAXBException {

        NetcoreWhatsAppMessage message = objectMapper.readValue(readPayload, NetcoreWhatsAppMessage.class);
        Mono<XMessage> xMessage = adapter.convertMessageToXMsg(message);

        StepVerifier.create(xMessage)
                .consumeNextWith(new Consumer<XMessage>() {
                    @Override
                    public void accept(XMessage xMessage) {
                        assertEquals("test", xMessage.getApp());
                        assertEquals("9415787824", xMessage.getFrom().getUserID());
                        assertEquals("A", xMessage.getAdapterId());
                        assertEquals("WhatsApp", xMessage.getChannelURI());
                        assertEquals("Netcore", xMessage.getProviderURI());
                        assertEquals("READ", xMessage.getMessageState().toString());
                    }
                })
                .expectComplete()
                .verify();
    }

    @Test
    public void sentPayloadParsing() throws JsonProcessingException, JAXBException {

        NetcoreWhatsAppMessage message = objectMapper.readValue(sentPayload, NetcoreWhatsAppMessage.class);
        Mono<XMessage> xMessage = adapter.convertMessageToXMsg(message);

        StepVerifier.create(xMessage)
                .consumeNextWith(new Consumer<XMessage>() {
                    @Override
                    public void accept(XMessage xMessage) {
                        assertEquals("test", xMessage.getApp());
                        assertEquals("9415787824", xMessage.getFrom().getUserID());
                        assertEquals("A", xMessage.getAdapterId());
                        assertEquals("WhatsApp", xMessage.getChannelURI());
                        assertEquals("Netcore", xMessage.getProviderURI());
                        assertEquals("SENT", xMessage.getMessageState().toString());
                    }
                })
                .expectComplete()
                .verify();
    }

    @Test
    public void deliveredPayloadParsing() throws JsonProcessingException, JAXBException {

        NetcoreWhatsAppMessage message = objectMapper.readValue(deliveredPayload, NetcoreWhatsAppMessage.class);
        Mono<XMessage> xMessage = adapter.convertMessageToXMsg(message);

        StepVerifier.create(xMessage)
                .consumeNextWith(new Consumer<XMessage>() {
                    @Override
                    public void accept(XMessage xMessage) {
                        assertEquals("test", xMessage.getApp());
                        assertEquals("9415787824", xMessage.getFrom().getUserID());
                        assertEquals("A", xMessage.getAdapterId());
                        assertEquals("WhatsApp", xMessage.getChannelURI());
                        assertEquals("Netcore", xMessage.getProviderURI());
                        assertEquals("DELIVERED", xMessage.getMessageState().toString());
                    }
                })
                .expectComplete()
                .verify();
    }

//    @Test
//    public void processOutBoundMessageF() throws Exception {
//        ArrayList<XMessageDAO> xMessageDAOArrayList = new ArrayList<>();
//        xMessageDAOArrayList.add(xMessageDAO);
//        when(xMessageRepo.findAllByUserIdOrderByTimestamp((String) notNull())).thenReturn(xMessageDAOArrayList);
//
//        NetcoreWhatsAppMessage message = objectMapper.readValue(simplePayload, NetcoreWhatsAppMessage.class);
////        XMessage xMessage = adapter.convertMessageToXMsg(message);
////        long start = System.currentTimeMillis();
////        for(int i=0;i<10000;i++){
////            Mono<Boolean> b = adapter.processOutBoundMessageF(xMessage);
////
////            StepVerifier.create(b)
////                    .expectNext(true)
////                    .expectComplete()
////                    .verify();
////        }
////        long end = System.currentTimeMillis();
////        System.out.println("Time Taken :-" + (end-start));
//    }

    @AfterAll
    static void teardown() {
        System.out.println("Teardown 43");
    }

}
