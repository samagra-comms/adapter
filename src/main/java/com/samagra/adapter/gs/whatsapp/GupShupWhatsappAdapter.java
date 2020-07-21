package com.samagra.adapter.gs.whatsapp;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.samagra.adapter.provider.factory.AbstractProvider;
import com.samagra.adapter.provider.factory.IProvider;
import com.samagra.common.Request.CommonMessage;
import com.samagra.utils.GupShupUtills;
import lombok.extern.slf4j.Slf4j;
import messagerosa.core.model.MessageID;
import messagerosa.core.model.SenderReceiverInfo;
import messagerosa.core.model.XMessage;
import messagerosa.core.model.XMessagePayload;
import org.apache.http.NameValuePair;
import org.apache.http.client.utils.URLEncodedUtils;
import org.apache.http.message.BasicNameValuePair;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import javax.xml.bind.JAXBException;
import java.nio.charset.Charset;
import java.util.*;

@Slf4j
@Qualifier("gupshupWhatsappAdapter")
@Service
public class GupShupWhatsappAdapter extends AbstractProvider implements IProvider {
//TODO channel provider strings set

    @Value("${provider.gupshup.whatsapp.appname}")
    private String gupshupWhatsappApp;

    @Value("{provider.gupshup.whatsapp.apikey}")
    private String gsApiKey;

    private final static String GUPSHUP_OUTBOUND = "https://api.gupshup.io/sm/api/v1/msg";

    @Autowired
    @Qualifier("rest")
    private RestTemplate restTemplate;

    @Override
    public XMessage convertMessageToXMsg(Object msg) throws JAXBException {
        GSWhatsAppMessage message = (GSWhatsAppMessage)msg;
        SenderReceiverInfo from = SenderReceiverInfo.builder().userID(message.getApp()).build();
        SenderReceiverInfo to = SenderReceiverInfo.builder().userID(message.getPayload().getSource()).build();

        XMessagePayload xmsgPayload = XMessagePayload.builder().text(message.getPayload().getPayload().getText())
                .build();

        MessageID messageId = MessageID.builder().gupshupMessageID(message.getPayload().getId()).build();
        XMessage xmessage = XMessage.builder().app(message.getApp())
                .to(to)
                .from(from)
                .channelURI("whatsapp")
                .providerURI("gupshup")
                .messageID(messageId)
                .timestamp(message.getTimestamp())
                .payload(xmsgPayload).build();
        return xmessage;
    }

    @Override
    public void processInBoundMessage(XMessage nextMsg) throws Exception {
        log.info("nextXmsg {}", new ObjectMapper().writeValueAsString(nextMsg));
        callOutBoundAPI(nextMsg);
    }


    public XMessage callOutBoundAPI(XMessage xMsg) throws Exception {
        log.info("next question to user is {}" , new ObjectMapper().writeValueAsString(xMsg));

        HashMap<String, String> params = new HashMap<String, String>();
        params.put("channel", xMsg.getChannelURI());
        params.put("source", xMsg.getFrom().getUserID());
        params.put("destination", xMsg.getTo().getUserID());
        params.put("src.name", "testingBotTemp");
        // params.put("type", "text");
        params.put("message",  xMsg.getPayload().getText());
        // params.put("isHSM", "false");

        String str2 =
                URLEncodedUtils.format(GupShupUtills.hashMapToNameValuePairList(params), '&', Charset.defaultCharset());

        log.info("Question for user: {}", xMsg.getPayload().getText());
        
        HttpEntity<String> request = new HttpEntity<String>(str2, getVerifyHttpHeader());
        restTemplate.getMessageConverters().add(GupShupUtills.getMappingJackson2HttpMessageConverter());
        restTemplate.postForObject(GUPSHUP_OUTBOUND, request, String.class);
        return null;
    }

    public  HttpHeaders getVerifyHttpHeader() throws Exception {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);
        headers.add("Cache-Control", "no-cache");
        headers.add("apikey", gsApiKey);
        return headers;
    }

    private HashMap<String, String> constructWhatsAppMessage(GSWhatsAppMessage message) {
        HashMap<String, String> params = new HashMap<String, String>();
        if (message.getPayload().getType().equals("message")
                && message.getPayload().getPayload().getType().equals("text")) {
            params.put("type", message.getPayload().getPayload().getType());
            params.put("text", message.getPayload().getPayload().getType());
            params.put("isHSM", String.valueOf(message.getPayload().getPayload().getHsm()));
        } else if (message.getPayload().getType().equals("message")
                && message.getPayload().getPayload().getType().equals("image")) {
            params.put("type", message.getPayload().getPayload().getType());
            params.put("originalUrl", message.getPayload().getPayload().getUrl());
            params.put("previewUrl", (message.getPayload().getPayload().getUrl()));
            if (message.getPayload().getPayload().getCaption() != null) {
                params.put("caption", message.getPayload().getPayload().getCaption());
            }
        } else if (message.getPayload().getType().equals("message")
                && (message.getPayload().getPayload().getType().equals("file")
                || message.getPayload().getPayload().getType().equals("audio")
                || message.getPayload().getPayload().getType().equals("video"))) {
            params.put("type", message.getPayload().getPayload().getType());
            params.put("url", message.getPayload().getPayload().getUrl());
            if (message.getPayload().getPayload().getFileName() != null) {
                params.put("fileName", message.getPayload().getPayload().getFileName());
            }
            if (message.getPayload().getPayload().getCaption() != null) {
                params.put("caption", message.getPayload().getPayload().getCaption());
            }
        }
        return params;
    }
}
