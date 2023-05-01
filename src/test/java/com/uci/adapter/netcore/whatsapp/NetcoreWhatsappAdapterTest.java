package com.uci.adapter.netcore.whatsapp;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.uci.adapter.AdapterTestConfiguration;
import com.uci.adapter.netcore.whatsapp.inbound.NetcoreWhatsAppMessage;
import com.uci.utils.BotService;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import messagerosa.core.model.SenderReceiverInfo;
import messagerosa.core.model.XMessage;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import static org.junit.jupiter.api.Assertions.assertEquals;

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
        adapter = new NetcoreWhatsappAdapter();
		adapter.setBotservice(botService);
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
    public void convertMessageToXMsgTest() throws JsonMappingException, JsonProcessingException {
    	String simplePayload = "{\"message_id\": \"ABEGkZlgQyWAAgo-sDVSUOa9jH0z\",\"from\": \"919960432580\",\"received_at\": \"1567090835\",\"context\": {\"ncmessage_id\": null,\"message_id\": null},\"message_type\": \"TEXT\",\"text_type\": {\"text\": \"Hi UCI\"}}";
		ObjectMapper objectMapper = new ObjectMapper();
		NetcoreWhatsAppMessage message = objectMapper.readValue(simplePayload, NetcoreWhatsAppMessage.class);
		
		Mono<XMessage> xmsg = adapter.convertMessageToXMsg(message);
		
		xmsg.log().subscribe(System.out::println,
				(e) -> System.err.println("---------------Exception occured in converting Message to XMessage-------------: " + e),
				() -> System.out.println("------------Convert Message to XMessage completed-----------"));
    }
    
    @Test
    public void processOutBoundMessageTest() throws JsonMappingException, JsonProcessingException {
    	String simplePayload = "{\"message_id\": \"ABEGkZlgQyWAAgo-sDVSUOa9jH0z\",\"from\": \"917597185708\",\"received_at\": \"1567090835\",\"context\": {\"ncmessage_id\": null,\"message_id\": null},\"message_type\": \"TEXT\",\"text_type\": {\"text\": \"Hello\"}}";
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
    
    @AfterAll
    static void teardown() {
        System.out.println("Teardown");
    }

}
