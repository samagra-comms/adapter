package com.uci.adapter.gs.sms;

import javax.xml.bind.JAXBException;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.client.RestTemplate;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.uci.adapter.netcore.whatsapp.NetcoreWhatsappAdapter;
import com.uci.adapter.netcore.whatsapp.inbound.NetcoreWhatsAppMessage;
import com.uci.utils.BotService;

import lombok.SneakyThrows;
import messagerosa.core.model.SenderReceiverInfo;
import messagerosa.core.model.XMessage;
import messagerosa.core.model.XMessagePayload;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

public class GupshupSMSAdapterTest {
	GupShupSMSAdapter adapter;

    @SneakyThrows
    @BeforeEach
    public void init() {
        adapter = GupShupSMSAdapter
                .builder()
                .restTemplate(new RestTemplate())
                .build();
    }
    
	@Test
    public void callOutBoundAPITest() throws JAXBException {
		XMessage xmsg = getXMessageForTest();
		
		try {
			XMessage response = adapter.callOutBoundAPI(xmsg);
			System.out.println(response);
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
    }
	
	/* XMessage object for testing */
    private XMessage getXMessageForTest() {
    	SenderReceiverInfo to = SenderReceiverInfo.builder().userID("917597185708").build();
		XMessagePayload payload = XMessagePayload.builder().text("Hey").build();
		return XMessage.builder().to(to).payload(payload).build();
    }
}	
