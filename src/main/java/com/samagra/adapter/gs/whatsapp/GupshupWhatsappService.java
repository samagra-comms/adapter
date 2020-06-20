package com.samagra.adapter.gs.whatsapp;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.xml.XmlMapper;
import com.samagra.adapter.gs.whatsapp.entity.GupshupMessageEntity;
import com.samagra.adapter.gs.whatsapp.entity.GupshupStateEntity;
import com.samagra.adapter.gs.whatsapp.repo.MessageRepository;
import com.samagra.adapter.gs.whatsapp.repo.StateRepository;
import com.samagra.adapter.provider.factory.AbstractProvider;
import com.samagra.adapter.provider.factory.IProvider;
import com.samagra.adapter.publisher.GupshupWhatsappOutboundPub;
import com.samagra.common.Request.GSWhatsAppMessage;
import lombok.extern.slf4j.Slf4j;
import messagerosa.core.model.XMessage;
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
@Qualifier("gupshupWhatsappService")
@Service
public class GupshupWhatsappService extends AbstractProvider implements IProvider {

    @Value("${provider.gupshup.whatsapp.appname}")
    private String gupshupWhatsappApp;

    @Value("{provider.gupshup.whatsapp.apikey}")
    private String gsApiKey;

    private final static String GUPSHUP_OUTBOUND = "https://api.gupshup.io/sm/api/v1/msg";

    @Autowired
    @Qualifier("rest")
    private RestTemplate restTemplate;

    @Autowired
    private StateRepository stateRepo;

    @Autowired
    private MessageRepository msgRepo;

//    @Autowired
//    private ODKPublisher odkPublisher;
//
    @Autowired
    private GupshupWhatsappOutboundPub gsWhatsAppPublisher;

    @Override
    public void processInBoundMessage(XMessage nextMsg)
            throws Exception {
        // factory for channels
        // db calls
        replaceUserState(nextMsg);
        appendNewResponse(nextMsg);

        log.info("nextXmsg {}", new ObjectMapper().writeValueAsString(nextMsg));

        boolean isLastResponse = false;
//            TODO ms3Response.getCurrentIndex().equals("endOfForm") ? true : false;

        if (isLastResponse) {
//            odkPublisher.send(new XmlMapper().writeValueAsString(nextMsg));
        } else {
//            String outMsg = new XmlMapper().writeValueAsString(nextMsg);
//            gsWhatsAppPublisher.send(outMsg);
            sendGupshupWhatsAppOutBound(nextMsg);
        }
    }
    private void appendNewResponse(XMessage body) throws JsonProcessingException {
        String message = "";
        GupshupMessageEntity msgEntity =
                msgRepo.findByPhoneNo(body.getTo().getUserIdentifier());

        if (msgEntity == null) {
            msgEntity = new GupshupMessageEntity();
            message = body.getPayload().getText();
            msgEntity.setPhoneNo(body.getTo().getUserIdentifier());
        } else {
            message = msgEntity.getMessage();
        }
        msgEntity.setMessage(message + body.getPayload().getText());
        msgEntity.setLastResponse(false);
//            TODO body.getCurrentIndex() == null ? true : false);

        msgRepo.save(msgEntity);
    }

    private void replaceUserState(XMessage body) throws JAXBException {
        GupshupStateEntity saveEntity =
                stateRepo.findByPhoneNo(body.getTo().getUserIdentifier());
        if (saveEntity == null) {
            saveEntity = new GupshupStateEntity();
        }
        saveEntity.setPhoneNo(body.getTo().getUserIdentifier());
        saveEntity.setPreviousPath("path");
//            TODO body.getCurrentIndex());
        saveEntity.setXmlPrevious(body.getUserState());
        saveEntity.setBotFormName(null);
        stateRepo.save(saveEntity);
    }

    private void sendGupshupWhatsAppOutBound(XMessage xMsg) throws Exception {
        log.info("next question to user is {}" , new ObjectMapper().writeValueAsString(xMsg));

        HashMap<String, String> params = new HashMap<String, String>();
        params.put("channel", xMsg.getChannelURI());
        params.put("source", xMsg.getFrom().getUserIdentifier());
        params.put("destination", xMsg.getTo().getUserIdentifier());
        params.put("src.name", "demobb");
        // params.put("type", "text");
        params.put("message",  xMsg.getPayload().getText());
        // params.put("isHSM", "false");

        String str2 =
                URLEncodedUtils.format(hashMapToNameValuePairList(params), '&', Charset.defaultCharset());

        log.info("Question for user: {}", xMsg.getPayload().getText());
        HttpEntity<String> request = new HttpEntity<String>(str2, getVerifyHttpHeader());
        restTemplate.getMessageConverters().add(getMappingJackson2HttpMessageConverter());
        restTemplate.postForObject(GUPSHUP_OUTBOUND, request, String.class);
    }
    public static List<NameValuePair> hashMapToNameValuePairList(HashMap<String, String> map) {
        List<NameValuePair> list = new ArrayList<NameValuePair>();
        for (Map.Entry<String, String> entry : map.entrySet()) {
            String key = entry.getKey();
            String value = entry.getValue();
            list.add(new BasicNameValuePair(key, value));
        }
        return list;
    }

    private HttpHeaders getVerifyHttpHeader() throws Exception {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);
        headers.add("Cache-Control", "no-cache");
        headers.add("apikey", gsApiKey);
        return headers;
    }

    public MappingJackson2HttpMessageConverter getMappingJackson2HttpMessageConverter() {
        MappingJackson2HttpMessageConverter mappingJackson2HttpMessageConverter =
                new MappingJackson2HttpMessageConverter();
        mappingJackson2HttpMessageConverter
                .setSupportedMediaTypes(Collections.singletonList(MediaType.APPLICATION_FORM_URLENCODED));
        return mappingJackson2HttpMessageConverter;
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
