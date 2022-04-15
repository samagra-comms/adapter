package com.uci.adapter.netcore.whatsapp;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.uci.adapter.AdapterTestConfiguration;
import com.uci.adapter.netcore.whatsapp.inbound.NetcoreWhatsAppMessage;
import com.uci.adapter.netcore.whatsapp.outbound.OutboundMessage;
import com.uci.utils.BotService;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import messagerosa.core.model.MediaCategory;
import messagerosa.core.model.SenderReceiverInfo;
import messagerosa.core.model.XMessage;
import org.mockito.Mockito;
import reactor.core.CoreSubscriber;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.*;

@Slf4j
//@ExtendWith(MockitoExtension.class)
@SpringBootTest(classes=AdapterTestConfiguration.class)
class NetcoreWhatsappAdapterTest {
	
    NetcoreWhatsappAdapter adapter;
    ObjectMapper objectMapper;

    @Autowired
    BotService botService;

    @SneakyThrows
    @BeforeEach
    public void init() {
        objectMapper = new ObjectMapper();
        adapter = Mockito.spy( NetcoreWhatsappAdapter
                    .builder()
                    .botservice(botService)
                    .build()
        );
    }

    @AfterAll
    static void teardown() {
        System.out.println("Teardown");
    }

    @Test
    public void timestampParsingREAD(){
        Long timestamp = adapter.getTimestamp("READ", "1558001218");
        assertEquals(Long.parseLong("1558001218000"), timestamp);
    }

    @Test
    public void timestampParsingREPLIED(){
        Long timestamp = adapter.getTimestamp(null, "1567090835");
        System.out.println(timestamp);
        assertEquals(Long.parseLong("1567090835000"), timestamp);
    }

    @Test
    public void getMessageStateTest() {
    	XMessage.MessageState state = adapter.getMessageState("SENT");
    	assertEquals(state, XMessage.MessageState.SENT);
    }

    @Test
    public void isInboundMediaMessageTest(){
        String type = "VIDEO";
        Boolean result = adapter.isInboundMediaMessage(type);
        assertEquals(result, true);
    }
    
    @Test 
    public void convertMessageToXMsgTest() throws JsonMappingException, JsonProcessingException {
    	// payload contain simeple text
        String simplePayload = "{\"message_id\": \"ABEGkZlgQyWAAgo-sDVSUOa9jH0z\",\"from\": \"919960432580\",\"received_at\": \"1567090835\",\"context\": {\"ncmessage_id\": null,\"message_id\": null},\"message_type\": \"TEXT\",\"text_type\": {\"text\": \"Hi UCI\"}}";
		ObjectMapper objectMapper = new ObjectMapper();
		NetcoreWhatsAppMessage message = objectMapper.readValue(simplePayload, NetcoreWhatsAppMessage.class);
		
		Mono<XMessage> xmsg = adapter.convertMessageToXMsg(message);
		
		xmsg.log().subscribe(System.out::println,
				(e) -> System.err.println("---------------Exception occured in converting Message to XMessage-------------: " + e),
				() -> System.out.println("------------Convert Message to XMessage completed-----------"));
    }

    @Test
    public void convertMessageToXMsgMediaTest() throws IOException {
        // payload contain media (image)
        String simplePayload = "{\"mobile\":\"917823807161\",\"messageId\":\"ABEGkXgjgHFhAgo-sOauiNCyN7Qz\",\"timestamp\":\"1649744193\",\"version\":0,\"type\":\"IMAGE\",\"imageType\":{\"mimeType\":\"image/jpeg\",\"sha256\":\"0cd07e978ae79c572fd113497baa5a2ded3e6f34019de24f6d53d7c64ad7b302\",\"id\":\"f2bedc20-46d5-4a57-9bb7-d7c3fb845525\"}}";
        ObjectMapper objectMapper = new ObjectMapper();
        NetcoreWhatsAppMessage message = objectMapper.readValue(simplePayload, NetcoreWhatsAppMessage.class);

        Map<String , Object> t = new HashMap<>();
        t.put("name", "abcd");
        t.put("url", "https://cdn.pixabay.com/photo/2020/06/01/22/23/eye-5248678__340.jpg");
        t.put("category", MediaCategory.IMAGE);
        t.put("size", 1000d);

        Mockito.doReturn(t).when(adapter).uploadInboundMediaFile(any(), any(), any());

        Mono<XMessage> xmsg = adapter.convertMessageToXMsg(message);

        xmsg.log().subscribe(System.out::println,
                (e) -> System.err.println("---------------Exception occured in converting Message to XMessage-------------: " + e),
                () -> System.out.println("------------Convert Message to XMessage completed-----------"));
    }

    @Test public void convertMessageToXMsgLocationTest() throws JsonProcessingException {
        // payload contain location
        String simplePayload = "{\"mobile\":\"917823807161\",\"messageId\":\"ABEGkXgjgHFhAhDFsF8-pis-ByeRsmRZh4Tz\",\"timestamp\":\"1649837600\",\"version\":0,\"type\":\"LOCATION\",\"location\":{\"latitude\":19.112976,\"longitude\":72.863625,\"address\":\"OppositeCourtyardMarriot,behindDivineChildSchool,Andheri-KurlaRd,Mumbai,Mahārāshtra400093\",\"url\":\"https://foursquare.com/v/5b915ce5f709c1002c09af17\",\"name\":\"MumbaiHouse\"}}";
        ObjectMapper objectMapper = new ObjectMapper();
        NetcoreWhatsAppMessage message = objectMapper.readValue(simplePayload, NetcoreWhatsAppMessage.class);

        Mono<XMessage> xmsg = adapter.convertMessageToXMsg(message);

        xmsg.log().subscribe(System.out::println,
                (e) -> System.err.println("---------------Exception occured in converting Message to XMessage-------------: " + e),
                () -> System.out.println("------------Convert Message to XMessage completed-----------"));
    }

    
    @Test
    public void processOutBoundMessageTest() throws JsonMappingException, JsonProcessingException {
    	String simplePayload = "{\"message_id\": \"ABEGkZlgQyWAAgo-sDVSUOa9jH0z\",\"from\": \"917823807161\",\"received_at\": \"1567090835\",\"context\": {\"ncmessage_id\": null,\"message_id\": null},\"message_type\": \"TEXT\",\"text_type\": {\"text\": \"Hello\"}}";
		ObjectMapper objectMapper = new ObjectMapper();
		NetcoreWhatsAppMessage message = objectMapper.readValue(simplePayload, NetcoreWhatsAppMessage.class);
		
		adapter.convertMessageToXMsg(message).subscribe(xmsg -> {
            SenderReceiverInfo from = xmsg.getFrom();
            SenderReceiverInfo to = xmsg.getTo();
            /* Switch from & to */
            xmsg.setFrom(to);
            xmsg.setTo(from);
            log.info("From: "+xmsg.getFrom().getUserID()+", to: "+xmsg.getTo().getUserID());
            try {
                /** Add NETCORE_WHATSAPP_AUTH_TOKEN in Env file,
                 * as we need this to send messaes to netcore, and recieve its response
                 */
				Mono<XMessage> response = adapter.processOutBoundMessageF(xmsg);

                StepVerifier.create(response.log()).expectNext(xmsg).verifyComplete();
			} catch (Exception e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		});
    }


    @Test
    public void processOutBoundMessageQuickReplyTest() throws JsonMappingException, JsonProcessingException {
        // XMessage contain Quick Reply Button
        String simplePayload = "{\"app\":\"TestConstraintBot\",\"messageType\":\"TEXT\",\"adapterId\":\"44a9df72-3d7a-4ece-94c5-98cf26307324\",\"messageId\":{\"channelMessageId\":\"ABEGkXgjgHFhAgo-sLIBzLABVkHR\"},\"to\":{\"userID\":\"7823807161\",\"campaignID\":\"TestConstraintBot\",\"bot\":false,\"broadcast\":false,\"deviceType\":\"PHONE\",\"deviceID\":\"91311fd1-5c1c-4f81-b9eb-2d259159554a\",\"encryptedDeviceID\":\"yOJcM+Gm7yVkKeQqPhdDKNb0wsmh8St/ty+pM5Q+4W4=\"},\"from\":{\"userID\":\"admin\",\"bot\":false,\"broadcast\":false},\"channelURI\":\"WhatsApp\",\"providerURI\":\"Netcore\",\"timestamp\":1649767704000,\"messageState\":\"REPLIED\",\"lastMessageID\":\"cfcccb40-ba5e-11ec-bb44-1df194a7fa74\",\"conversationLevel\":[2],\"payload\":{\"text\":\"Whatisyourgender?-CorrectQuickReplyButton\\n\\n\",\"buttonChoices\":[{\"key\":\"1\",\"text\":\"1Male\"},{\"key\":\"2\",\"text\":\"2Female\"}],\"stylingTag\":\"QUICKREPLYBTN\"},\"provider\":\"Netcore\",\"channel\":\"WhatsApp\"}";
        ObjectMapper objectMapper = new ObjectMapper();
        XMessage message = objectMapper.readValue(simplePayload, XMessage.class);
        Mono<XMessage> mockResponse = Mono.just(message).cache();

        Mockito
                .doReturn(mockResponse)
                .when(adapter).sendOutboundMessage(any(OutboundMessage.class), any(NewNetcoreService.class), any(XMessage.class));

        Mono<XMessage> response = adapter.processOutBoundMessageF(message);
        StepVerifier.create(response.log()).expectNext(message).verifyComplete();
    }

    @Test
    public void processOutBoundMessageListTest() throws JsonMappingException, JsonProcessingException {
        // XMessage contain List Message
        String simplePayload = "{\"app\":\"TestConstraintBot\",\"messageType\":\"TEXT\",\"adapterId\":\"44a9df72-3d7a-4ece-94c5-98cf26307324\",\"messageId\":{\"channelMessageId\":\"ABEGkXgjgHFhAgo-sCO6Xuf_dqtH\"},\"to\":{\"userID\":\"7823807161\",\"campaignID\":\"TestConstraintBot\",\"bot\":false,\"broadcast\":false,\"deviceType\":\"PHONE\",\"deviceID\":\"91311fd1-5c1c-4f81-b9eb-2d259159554a\",\"encryptedDeviceID\":\"yOJcM+Gm7yVkKeQqPhdDKNb0wsmh8St/ty+pM5Q+4W4=\"},\"from\":{\"userID\":\"admin\",\"bot\":false,\"broadcast\":false},\"channelURI\":\"WhatsApp\",\"providerURI\":\"Netcore\",\"timestamp\":1649768023000,\"messageState\":\"REPLIED\",\"lastMessageID\":\"8fbb29b0-ba5f-11ec-bb44-1df194a7fa74\",\"conversationLevel\":[5],\"payload\":{\"text\":\"Selectthirdskillfromlist?-CorrectList\\n\\n\",\"buttonChoices\":[{\"key\":\"1\",\"text\":\"1c\"},{\"key\":\"2\",\"text\":\"2photoshop\"},{\"key\":\"3\",\"text\":\"3java\"},{\"key\":\"4\",\"text\":\"4python\"},{\"key\":\"5\",\"text\":\"5html\"},{\"key\":\"6\",\"text\":\"6css\"},{\"key\":\"7\",\"text\":\"7javascript\"},{\"key\":\"8\",\"text\":\"8reactjs\"},{\"key\":\"9\",\"text\":\"9nodejs\"},{\"key\":\"10\",\"text\":\"10angularjs\"}],\"stylingTag\":\"LIST\"},\"provider\":\"Netcore\",\"channel\":\"WhatsApp\"}";
        ObjectMapper objectMapper = new ObjectMapper();
        XMessage message = objectMapper.readValue(simplePayload, XMessage.class);
        Mono<XMessage> mockResponse = Mono.just(message).cache();

        Mockito
                .doReturn(mockResponse)
                .when(adapter).sendOutboundMessage(any(OutboundMessage.class), any(NewNetcoreService.class), any(XMessage.class));

        Mono<XMessage> response = adapter.processOutBoundMessageF(message);
        StepVerifier.create(response.log()).expectNext(message).verifyComplete();
    }

    @Test
    public void processOutBoundMessageMediaTest() throws JsonMappingException, JsonProcessingException {
        // XMessage contain media
        String simplePayload = "{\"app\":\"UCItestlist3\",\"messageType\":\"TEXT\",\"adapterId\":\"44a9df72-3d7a-4ece-94c5-98cf26307324\",\"messageId\":{\"channelMessageId\":\"ABEGkXgjgHFhAgo-sDhd_62GYZZZ\"},\"to\":{\"userID\":\"7823807161\",\"campaignID\":\"UCItestlist3\",\"bot\":false,\"broadcast\":false,\"deviceType\":\"PHONE\",\"deviceID\":\"91311fd1-5c1c-4f81-b9eb-2d259159554a\",\"encryptedDeviceID\":\"yOJcM+Gm7yVkKeQqPhdDKNb0wsmh8St/ty+pM5Q+4W4=\"},\"from\":{\"userID\":\"admin\",\"bot\":false,\"broadcast\":false},\"channelURI\":\"WhatsApp\",\"providerURI\":\"Netcore\",\"timestamp\":1649838131000,\"messageState\":\"REPLIED\",\"lastMessageID\":\"611c1ff0-bb00-11ec-898f-6d62c3880870\",\"conversationLevel\":[3,1],\"payload\":{\"text\":\"testing-1.jpg\\n\\n\",\"stylingTag\":\"IMAGE\",\"flow\":\"employerReg\",\"questionIndex\":2,\"mediaCaption\":\"Mobile\"},\"provider\":\"Netcore\",\"channel\":\"WhatsApp\"}";
        ObjectMapper objectMapper = new ObjectMapper();
        XMessage message = objectMapper.readValue(simplePayload, XMessage.class);
        Mono<XMessage> mockResponse = Mono.just(message).cache();

        Mockito
                .doReturn(mockResponse)
                .when(adapter).sendOutboundMessage(any(OutboundMessage.class), any(NewNetcoreService.class), any(XMessage.class));

        Mono<XMessage> response = adapter.processOutBoundMessageF(message);
        StepVerifier.create(response.log()).expectNext(message).verifyComplete();
    }



}
