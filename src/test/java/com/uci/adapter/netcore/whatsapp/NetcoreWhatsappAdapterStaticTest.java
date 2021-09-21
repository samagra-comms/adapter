package com.uci.adapter.netcore.whatsapp;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.uci.adapter.AdapterTestConfiguration;
import com.uci.adapter.netcore.whatsapp.inbound.NetcoreMessageFormat;
import com.uci.adapter.netcore.whatsapp.inbound.NetcoreWhatsAppMessage;
import com.uci.dao.models.XMessageDAO;
import com.uci.dao.utils.XMessageDAOUtils;
import com.uci.utils.BotService;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import messagerosa.core.model.XMessage;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.event.annotation.BeforeTestClass;
import org.springframework.web.client.RestTemplate;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.notNull;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.function.Consumer;

//@Slf4j
//@ExtendWith(MockitoExtension.class)
@SpringBootTest(classes=AdapterTestConfiguration.class)
class NetcoreWhatsappAdapterStaticTest {
	
    NetcoreWhatsappAdapter adapter;
    ObjectMapper objectMapper;
    String simplePayload, readPayload, sentPayload, deliveredPayload;

    @Autowired
    BotService botService;

    @SneakyThrows
    @BeforeEach
    public void init() {

        objectMapper = new ObjectMapper();
        adapter = NetcoreWhatsappAdapter
                .builder()
                .botservice(botService)
                .build();
    }

//    static void setFinalStatic(Field field, Object newValue) throws Exception {
//        field.setAccessible(true);        
//        Field modifiersField = Field.class.getDeclaredField("modifiers");
//        modifiersField.setAccessible(true);
//        modifiersField.setInt(field, field.getModifiers() & ~Modifier.FINAL);
//        field.set(null, newValue);
//    }
    
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

//    	StepVerifier.create(xmsg.log()).verifyComplete();
    
    }
    
    @Test
    public void processOutBoundMessageTest() throws JsonMappingException, JsonProcessingException {
    	String simplePayload = "{\"message_id\": \"ABEGkZlgQyWAAgo-sDVSUOa9jH0z\",\"from\": \"917597185708\",\"received_at\": \"1567090835\",\"context\": {\"ncmessage_id\": null,\"message_id\": null},\"message_type\": \"TEXT\",\"text_type\": {\"text\": \"Hello\"}}";
		ObjectMapper objectMapper = new ObjectMapper();
		NetcoreWhatsAppMessage message = objectMapper.readValue(simplePayload, NetcoreWhatsAppMessage.class);
		
		adapter.convertMessageToXMsg(message).subscribe(xmsg -> {
			XMessage replyXmsg;
			try {
				Mono<XMessage> response = adapter.processOutBoundMessageF(xmsg);
				
				StepVerifier.create(response).expectComplete().verify();
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
