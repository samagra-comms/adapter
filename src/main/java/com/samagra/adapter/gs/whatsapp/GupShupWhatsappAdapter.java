package com.samagra.adapter.gs.whatsapp;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.samagra.adapter.provider.factory.AbstractProvider;
import com.samagra.adapter.provider.factory.IProvider;
import com.samagra.utils.GupShupUtills;
import lombok.extern.slf4j.Slf4j;
import messagerosa.core.model.MessageId;
import messagerosa.core.model.SenderReceiverInfo;
import messagerosa.core.model.XMessage;
import messagerosa.core.model.XMessagePayload;
import messagerosa.dao.XMessageDAO;
import messagerosa.dao.XMessageDAOUtills;
import messagerosa.dao.XMessageRepo;
import org.apache.http.client.utils.URLEncodedUtils;
import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
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

    @Value("${provider.gupshup.whatsapp.apikey}")
    private String gsApiKey;

    private final static String GUPSHUP_OUTBOUND = "https://api.gupshup.io/sm/api/v1/msg";

    @Autowired
    @Qualifier("rest")
    private RestTemplate restTemplate;

    @Autowired
    @Qualifier("gupshupWhatsappAdapter")
    private GupShupWhatsappAdapter gupShupWhatsappAdapter;

    @Autowired
    public XMessageRepo xmsgRepo;

    @Override
    public XMessage convertMessageToXMsg(Object msg) throws JAXBException {
        GSWhatsAppMessage message = (GSWhatsAppMessage) msg;
        SenderReceiverInfo from = SenderReceiverInfo.builder().build();
        SenderReceiverInfo to = SenderReceiverInfo.builder().userID("admin").build();

        XMessage.MessageState messageState = XMessage.MessageState.REPLIED;
        MessageId messageIdentifier = MessageId.builder().build();

        XMessagePayload xmsgPayload = XMessagePayload.builder().build();
        long lastMsgId = 0;

        //TODO: Update "thread" and "lastMessageID"; Get these from the database.

        if (message.getType().equals("message-event")) {
            String payloadType = message.getPayload().getType();
            xmsgPayload.setText("");
            from.setUserID(message.getPayload().getDestination().substring(2));
            switch (payloadType) {
                case "enqueued":
                    messageState = XMessage.MessageState.ENQUEUED;
                    messageIdentifier.setWhatsappMessageId(message.getPayload().getPayload().getWhatsappMessageId());
                    messageIdentifier.setGupshupMessageId(message.getPayload().getId());
                    break;
                case "sent":
                    messageIdentifier.setWhatsappMessageId(message.getPayload().getId());
                    messageIdentifier.setGupshupMessageId(message.getPayload().getGsId());
                    messageState = XMessage.MessageState.SENT;
                    break;
                case "delivered":
                    messageIdentifier.setWhatsappMessageId(message.getPayload().getId());
                    messageIdentifier.setGupshupMessageId(message.getPayload().getGsId());
                    messageState = XMessage.MessageState.DELIVERED;
                    break;
                case "read":
                    messageIdentifier.setWhatsappMessageId(message.getPayload().getId());
                    messageIdentifier.setGupshupMessageId(message.getPayload().getGsId());
                    messageState = XMessage.MessageState.READ;
                    break;
                case "failed":
                    messageIdentifier.setWhatsappMessageId(message.getPayload().getId());
                    messageIdentifier.setGupshupMessageId(message.getPayload().getGsId());
                    messageState = XMessage.MessageState.FAILED_TO_DELIVER;
                    break;
                default:
                    messageState = XMessage.MessageState.REPLIED;
                    break;
            }
        } else if (message.getType().equals("user-event")) {
            String payloadType = message.getPayload().getType();
            xmsgPayload.setText("");
            switch (payloadType) {
                case "opted-in":
                    from.setUserID(message.getPayload().getPhone().substring(2));
                    messageState = XMessage.MessageState.OPTED_IN;
                    break;
                case "opted-out":
                    from.setUserID(message.getPayload().getPhone().substring(2));
                    messageState = XMessage.MessageState.OPTED_OUT;
                    break;
            }
        } else {
            //Actual Message with payload (User response)
            xmsgPayload.setText(message.getPayload().getPayload().getText());
            messageIdentifier.setGupshupMessageId(message.getPayload().getId());
            from.setUserID(message.getPayload().getSource().substring(2));
            List<XMessageDAO> msg1 = xmsgRepo.findAllByUserIdOrderByTimestamp(from.getUserID());
            if (msg1.size() > 0) {
                XMessageDAO msg0 = msg1.get(0);
                lastMsgId = msg0.getId();
            }
        }
        XMessage xmessage = XMessage.builder().app(message.getApp())
                .to(to)
                .from(from)
                .channelURI("WhatsApp")
                .providerURI("gupshup")
                .messageState(messageState)
                .messageId(messageIdentifier)
                .timestamp(message.getTimestamp()/1000)
                .payload(xmsgPayload)
                .lastMessageID(String.valueOf(lastMsgId)).build();
        return xmessage;
    }

    @Override
    public void processInBoundMessage(XMessage nextMsg) throws Exception {
        log.info("nextXmsg {}", new ObjectMapper().writeValueAsString(nextMsg));
        callOutBoundAPI(nextMsg);
    }


    public XMessage callOutBoundAPI(XMessage xMsg) throws Exception {
        log.info("next question to user is {}", new ObjectMapper().writeValueAsString(xMsg));

        HashMap<String, String> params = new HashMap<String, String>();
        params.put("channel", xMsg.getChannelURI().toLowerCase());
        params.put("source", "917834811114");
        params.put("destination", "91" + xMsg.getTo().getUserID());
        params.put("src.name", gupshupWhatsappApp);
        // params.put("type", "text");
        params.put("message", xMsg.getPayload().getText());
        // params.put("isHSM", "false");

        String str2 = URLEncodedUtils.format(
                GupShupUtills.hashMapToNameValuePairList(params),
                '&', Charset.defaultCharset()
        );

        HttpEntity<String> request = new HttpEntity<String>(str2, getVerifyHttpHeader());
        restTemplate.getMessageConverters().add(GupShupUtills.getMappingJackson2HttpMessageConverter());
        String result = restTemplate.postForObject(GUPSHUP_OUTBOUND, request, String.class);

        JSONObject json = new JSONObject(result);
        xMsg.setMessageId(MessageId.builder().gupshupMessageId(json.getString("messageId")).build());

        XMessageDAO dao = XMessageDAOUtills.convertXMessageToDAO(xMsg);
        xmsgRepo.save(dao);

        log.error(result);
        return xMsg;
    }

    public HttpHeaders getVerifyHttpHeader() throws Exception {
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
