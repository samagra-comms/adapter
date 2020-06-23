package com.samagra.adapter.gs.whatsapp;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.samagra.adapter.provider.factory.AbstractProvider;
import com.samagra.adapter.provider.factory.IProvider;
import com.samagra.common.Request.GSWhatsAppMessage;
import lombok.extern.slf4j.Slf4j;
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
@Qualifier("gupshupWhatsappService")
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

    @Autowired
    @Qualifier("gupshupWhatsappService")
    private GupShupWhatsappAdapter gsWhatsappService;

    public XMessage convertMessageToXMsg(GSWhatsAppMessage message) throws JAXBException {
        SenderReceiverInfo from = SenderReceiverInfo.builder().userIdentifier(message.getApp()).build();
        SenderReceiverInfo to = SenderReceiverInfo.builder().userIdentifier(message.getPayload().getSource()).build();

        XMessagePayload xmsgPayload = XMessagePayload.builder().text(message.getPayload().getPayload().getText())
                .build();

        XMessage xmessage = XMessage.builder().to(to).from(from).channelURI("whatsapp").providerURI("gupshup")
                .messageId(message.getPayload().getId()).timestamp(message.getTimestamp().toString())
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
//        return restTemplate;
        restTemplate.postForObject(GUPSHUP_OUTBOUND, request, String.class);
        return null;
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
