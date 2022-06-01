package com.uci.adapter.gs.sms;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.uci.adapter.gs.sms.outbound.GupshupSMSResponse;
import com.uci.adapter.gs.whatsapp.GSWhatsappOutBoundResponse;
import com.uci.adapter.provider.factory.AbstractProvider;
import com.uci.adapter.provider.factory.IProvider;
import com.uci.adapter.utils.GupShupUtills;

import lombok.Builder;
import lombok.extern.slf4j.Slf4j;
import messagerosa.core.model.MessageId;
import messagerosa.core.model.XMessage;
import org.apache.http.client.utils.URLEncodedUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;
import reactor.core.publisher.Mono;

import javax.xml.bind.JAXBException;
import java.net.URI;
import java.nio.charset.Charset;
import java.util.*;

@Slf4j
@Service
@Qualifier("gupshupSMSAdapter")
public class GupShupSMSAdapter  extends AbstractProvider implements IProvider {

    @Autowired
    @Qualifier("rest")
    private RestTemplate restTemplate;

    private final static String GUPSHUP_SMS_OUTBOUND = "http://enterprise.smsgupshup.com/GatewayAPI/rest";

    /**
     * Convert Gupshsup Message Format to XMessage Format for inbound
     * @param message
     * @return
     * @throws JAXBException
     */
    @Override
    public Mono<XMessage> convertMessageToXMsg(Object message) throws JAXBException{
        return null;
    }

    /* Not in use */
//    @Override
//    public void processOutBoundMessage(XMessage xMsg) throws Exception {
//        callOutBoundAPI(xMsg);
//    }

    /**
     * Process outbound message - send outbound message & Mono xmsg
     * @param xMsg
     * @return
     * @throws Exception
     */
    @Override
    public Mono<XMessage> processOutBoundMessageF(XMessage xMsg) throws Exception {
        return Mono.just(callOutBoundAPI(xMsg));
    }

    /**
     * Call gupshup sms outbound sms to send sms message to user & return xmsg
     * @param xMsg
     * @return
     * @throws Exception
     */
    public XMessage callOutBoundAPI(XMessage xMsg) throws Exception {
        UriComponentsBuilder builder = UriComponentsBuilder.fromHttpUrl(GUPSHUP_SMS_OUTBOUND);

        builder.queryParam("method", "SendMessage");
        builder.queryParam("send_to", xMsg.getTo().getUserID());
        builder.queryParam("msg",  xMsg.getPayload().getText());
        builder.queryParam("msg_type", "Text");
        builder.queryParam("messageId", "123456781");
        builder.queryParam("userid", "2000164521");
        builder.queryParam("auth_scheme", "plain");
        builder.queryParam("password", "samagra_15");
        builder.queryParam("v", "1.1");
        builder.queryParam("format", "json");
        builder.queryParam("data_encoding", "text");
        builder.queryParam("extra", "Samagra");

        URI expanded = URI.create(builder.toUriString());
        log.info(expanded.toString());

        RestTemplate restTemplate = new RestTemplate();
        GupshupSMSResponse response = restTemplate.getForObject(expanded, GupshupSMSResponse.class);
        try {
            log.info("response ================{}", new ObjectMapper().writeValueAsString(response));
        } catch (JsonProcessingException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
            log.error("Error in callOutBoundAPI for objectmapper: "+e.getMessage());
        }
        xMsg.setMessageId(MessageId.builder().channelMessageId(response.getResponse().getId()).build());
        xMsg.setMessageState(XMessage.MessageState.SENT);

        return xMsg;
    }
}
