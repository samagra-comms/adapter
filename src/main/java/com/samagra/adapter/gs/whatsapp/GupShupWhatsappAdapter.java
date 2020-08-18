package com.samagra.adapter.gs.whatsapp;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.samagra.adapter.provider.factory.AbstractProvider;
import com.samagra.adapter.provider.factory.IProvider;
import com.samagra.user.CampaignService;
import com.samagra.utils.GupShupUtills;
import lombok.extern.slf4j.Slf4j;
import messagerosa.core.model.MessageId;
import messagerosa.core.model.SenderReceiverInfo;
import messagerosa.core.model.XMessage;
import messagerosa.core.model.XMessagePayload;
import messagerosa.dao.XMessageDAO;
import messagerosa.dao.XMessageDAOUtills;
import messagerosa.dao.XMessageRepo;
import messagerosa.xml.XMessageParser;
import org.apache.http.client.utils.URLEncodedUtils;
import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import javax.xml.bind.JAXBException;
import java.io.ByteArrayInputStream;
import java.nio.charset.Charset;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.time.temporal.TemporalField;
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

//    private final static String GUPSHUP_OUTBOUND = "https://api.gupshup.io/sm/api/v1/msg";

    private final static String GUPSHUP_OUTBOUND = "https://media.smsgupshup.com/GatewayAPI/rest";
    @Autowired
    @Qualifier("rest")
    private RestTemplate restTemplate;

    @Autowired
    @Qualifier("gupshupWhatsappAdapter")
    private GupShupWhatsappAdapter gupShupWhatsappAdapter;

    @Autowired
    public XMessageRepo xmsgRepo;

    @Override
    public XMessage convertMessageToXMsg(Object msg) throws JAXBException, JsonProcessingException {
        GSWhatsAppMessage message = (GSWhatsAppMessage) msg;
        SenderReceiverInfo from = SenderReceiverInfo.builder().build();
        SenderReceiverInfo to = SenderReceiverInfo.builder().userID("admin").build();

        XMessage.MessageState messageState = XMessage.MessageState.REPLIED;
        MessageId messageIdentifier = MessageId.builder().build();

        XMessagePayload xmsgPayload = XMessagePayload.builder().build();
        long lastMsgId = 0;

        //TODO: Update "thread" and "lastMessageID"; Get these from the database.
//        reportMsg {"externalId":"4179751116907233291-398941242659789719","eventType":"SENT"
//        ,"eventTs":"1597776789000","destAddr":"919718908699","srcAddr":"SDTEXT",
//        "cause":"SENT","errorCode":"025","channel":"WHATSAPP"}

        if (message.getResponse() != null ) {
            String reportResponse = message.getResponse();
            List<GSWhatsappReport> participantJsonList = new ObjectMapper().readValue(reportResponse, new TypeReference<List<GSWhatsappReport>>(){});
            for(GSWhatsappReport reportMsg :participantJsonList) {
                log.info("reportMsg {}",new ObjectMapper().writeValueAsString(reportMsg));
                String payloadType = reportMsg.getEventType();
                xmsgPayload.setText("");
                messageIdentifier.setChannelMessageId(reportMsg.getExternalId());
                switch (payloadType) {
                    case "SENT":
                        messageState = XMessage.MessageState.SENT;
                        break;
                    case "DELIVERED":
                        messageState = XMessage.MessageState.DELIVERED;
                        break;
                    case "READ":
                        messageState = XMessage.MessageState.READ;
                        break;
//                case "failed":
//                    messageIdentifier.setChannelMessageId(message.getExternalId());
//                    messageState = XMessage.MessageState.FAILED_TO_DELIVER;
//                    break;
                    default:
                        messageState = XMessage.MessageState.FAILED_TO_DELIVER;
                        break;
                }
//            }
//        } else
//            if (message.getType().equals("user-event")) {
//            String payloadType = message.getPayload().getType();
//            xmsgPayload.setText("");
//            switch (payloadType) {
//                case "opted-in":
//                    from.setUserID(message.getPayload().getPhone().substring(2));
//                    messageState = XMessage.MessageState.OPTED_IN;
//                    CampaignService.addUserToCampaign(from.getUserID(), message.getApp());
//                    break;
//                case "opted-out":
//                    from.setUserID(message.getPayload().getPhone().substring(2));
//                    messageState = XMessage.MessageState.OPTED_OUT;
//                    break;
            }
        } else if(message.getType().equals("text")){
//            Actual Message with payload (user response)
            from.setUserID(message.getMobile().substring(2));
            if(message.getType().equals("OPT_IN")){
                xmsgPayload.setText("");
                messageState = XMessage.MessageState.OPTED_IN;
            }else if(message.getType().equals("OPT_OUT")){
                xmsgPayload.setText("stop-wa");
                messageState = XMessage.MessageState.OPTED_OUT;
             }else{
                messageState = XMessage.MessageState.REPLIED;
                xmsgPayload.setText(message.getText());
                messageIdentifier.setChannelMessageId(message.getMessageId());
                from.setUserID(message.getMobile().substring(2));
            }
            List<XMessageDAO> msg1 = xmsgRepo.findAllByUserIdOrderByTimestamp(from.getUserID());
            if (msg1.size() > 0) {
                XMessageDAO msg0 = msg1.get(0);
                lastMsgId = msg0.getId();
            }
        }
        return XMessage.builder()
//                .app(message.getApp())
                .to(to)
                .from(from)
                .channelURI("WhatsApp")
                .providerURI("gupshup")
                .messageState(messageState)
                .messageId(messageIdentifier)
                .timestamp(message.getTimestamp() == null ? Timestamp.valueOf(LocalDateTime.now()).getTime(): message.getTimestamp())
                .payload(xmsgPayload)
                .lastMessageID(String.valueOf(lastMsgId)).build();
    }

    @Override
    public void processInBoundMessage(XMessage nextMsg) throws Exception {
        log.info("nextXmsg {}", new ObjectMapper().writeValueAsString(nextMsg));
        callOutBoundAPI(nextMsg);
    }
public static void main(String arg[]) throws Exception {
    GupShupWhatsappAdapter adapter = new GupShupWhatsappAdapter();
    String x = "<xMessage>\n" +
            "    <channel>WhatsApp</channel>\n" +
            "    <channelURI>WhatsApp</channelURI>\n" +
            "    <from>\n" +
            "        <bot>false</bot>\n" +
            "        <broadcast>false</broadcast>\n" +
            "        <userID>9718908699</userID>\n" +
            "    </from>\n" +
            "    <lastMessageID>0</lastMessageID>\n" +
            "    <messageId/>\n" +
            "    <messageState>REPLIED</messageState>\n" +
            "    <payload>\n" +
            "        <text>HI</text>\n" +
            "    </payload>\n" +
            "    <provider>gupshup</provider>\n" +
            "    <providerURI>gupshup</providerURI>\n" +
            "    <timestamp>1597777963000</timestamp>\n" +
            "    <to>\n" +
            "        <bot>false</bot>\n" +
            "        <broadcast>false</broadcast>\n" +
            "        <userID>admin</userID>\n" +
            "    </to>\n" +
            "</xMessage>";
    XMessage currentXmsg = XMessageParser.parse(new ByteArrayInputStream(x.getBytes()));

    adapter.callOutBoundAPI(currentXmsg);
}

    public XMessage callOutBoundAPI(XMessage xMsg) throws Exception {
        log.info("next question to user is {}", new ObjectMapper().writeValueAsString(xMsg));

        HashMap<String, String> params = new HashMap<String, String>();
        params.put("userid", "2000193033");
        params.put("password", "v@MPj33Q");
        params.put("v", "1.1");
        params.put("format", "json");
        params.put("auth_scheme", "plain");

        if(xMsg.getMessageState().equals(XMessage.MessageState.OPTED_IN)) {
            params.put("channel", xMsg.getChannelURI().toLowerCase());
            params.put("phone_number", "91" + xMsg.getTo().getUserID());
            params.put("method", "OPT_IN");
        }else if(xMsg.getMessageState().equals(XMessage.MessageState.REPLIED)){
            params.put("method", "SendMessage");
            params.put("send_to", "91" + xMsg.getFrom().getUserID());
            params.put("msg", xMsg.getPayload().getText());
            params.put("msg_type", "TEXT");
        }

        HttpHeaders headers = new HttpHeaders();
        headers.set("Accept", MediaType.APPLICATION_JSON_VALUE);

        UriComponentsBuilder builder = UriComponentsBuilder.fromHttpUrl(GUPSHUP_OUTBOUND)
                .queryParam("userid", "2000193033")
                .queryParam("password", "v@MPj33Q")
                .queryParam("v", "1.1")
                .queryParam("format", "json")
                .queryParam("auth_scheme", "plain")
                .queryParam("method", "SendMessage").queryParam("send_to", "91" + xMsg.getFrom().getUserID())
                .queryParam("msg", xMsg.getPayload().getText()).queryParam("msg_type", "TEXT");


        HttpEntity<?> entity = new HttpEntity<>(headers);
        RestTemplate restTemplate = new RestTemplate();
        HttpEntity<String> response = restTemplate.exchange(
                builder.toUriString(),
                HttpMethod.GET,
                entity,
                String.class);
    log.info("response ================{}",response);
//        // params.put("phone_number", "91" + xMsg.getTo().getUserID());
//        // params.put("src.name", gupshupWhatsappApp);
//        // params.put("type", "text");
//        // params.put("isHSM", "false");
//
//        String str2 = URLEncodedUtils.format(
//                GupShupUtills.hashMapToNameValuePairList(params),
//                '&', Charset.defaultCharset()
//        );
//
////        HttpEntity<String> request = new HttpEntity<String>(str2, getVerifyHttpHeader());
//        HttpEntity<String> request = new HttpEntity<String>(str2);
//
//        restTemplate.getMessageConverters().add(GupShupUtills.getMappingJackson2HttpMessageConverter());
//        String result = restTemplate.postForObject(GUPSHUP_OUTBOUND, request, String.class);
//    log.info("set message {}",result);
//        JSONObject json = new JSONObject(result);
//
//        xMsg.setMessageId(MessageId.builder().channelMessageId(response.getBody()  ("messageId")).build());

        XMessageDAO dao = XMessageDAOUtills.convertXMessageToDAO(xMsg);
        xmsgRepo.save(dao);

//        log.error(result);
        return xMsg;
    }

    public HttpHeaders getVerifyHttpHeader() throws Exception {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);
        headers.add("Cache-Control", "no-cache");
        headers.add("apikey", gsApiKey);
        return headers;
    }

//    private HashMap<String, String> constructWhatsAppMessage(GSWhatsAppMessage message) {
//        HashMap<String, String> params = new HashMap<String, String>();
//        if (message.getPayload().getType().equals("message")
//                && message.getPayload().getPayload().getType().equals("text")) {
//            params.put("type", message.getPayload().getPayload().getType());
//            params.put("text", message.getPayload().getPayload().getType());
//            params.put("isHSM", String.valueOf(message.getPayload().getPayload().getHsm()));
//        } else if (message.getPayload().getType().equals("message")
//                && message.getPayload().getPayload().getType().equals("image")) {
//            params.put("type", message.getPayload().getPayload().getType());
//            params.put("originalUrl", message.getPayload().getPayload().getUrl());
//            params.put("previewUrl", (message.getPayload().getPayload().getUrl()));
//            if (message.getPayload().getPayload().getCaption() != null) {
//                params.put("caption", message.getPayload().getPayload().getCaption());
//            }
//        } else if (message.getPayload().getType().equals("message")
//                && (message.getPayload().getPayload().getType().equals("file")
//                || message.getPayload().getPayload().getType().equals("audio")
//                || message.getPayload().getPayload().getType().equals("video"))) {
//            params.put("type", message.getPayload().getPayload().getType());
//            params.put("url", message.getPayload().getPayload().getUrl());
//            if (message.getPayload().getPayload().getFileName() != null) {
//                params.put("fileName", message.getPayload().getPayload().getFileName());
//            }
//            if (message.getPayload().getPayload().getCaption() != null) {
//                params.put("caption", message.getPayload().getPayload().getCaption());
//            }
//        }
//        return params;
//    }
}
