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
import org.springframework.web.util.UriTemplate;

import javax.xml.bind.JAXBException;
import java.io.ByteArrayInputStream;
import java.net.URI;
import java.net.URLDecoder;
import java.net.URLEncoder;
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
        String appName = "";


        if (message.getResponse() != null) {
            String reportResponse = message.getResponse();
            List<GSWhatsappReport> participantJsonList = new ObjectMapper().readValue(reportResponse, new TypeReference<List<GSWhatsappReport>>() {
            });
            for (GSWhatsappReport reportMsg : participantJsonList) {
                log.info("reportMsg {}", new ObjectMapper().writeValueAsString(reportMsg));
                String eventType = reportMsg.getEventType();
                xmsgPayload.setText("");
                messageIdentifier.setChannelMessageId(reportMsg.getExternalId());
                from.setUserID(reportMsg.getDestAddr().substring(2));
                appName = getAppName(from, message.getText());
                switch (eventType) {
                    case "SENT":
                        messageState = XMessage.MessageState.SENT;
                        break;
                    case "DELIVERED":
                        messageState = XMessage.MessageState.DELIVERED;
                        break;
                    case "READ":
                        messageState = XMessage.MessageState.READ;
                        break;
                    default:
                        messageState = XMessage.MessageState.FAILED_TO_DELIVER;
                        break;
                }
            }
        } else if (message.getType().equals("text")) {
            //Actual Message with payload (user response)
            from.setUserID(message.getMobile().substring(2));
            appName = getAppName(from, message.getText());
            messageIdentifier.setReplyId(message.getReplyId());
            if (message.getType().equals("OPT_IN")) {
                messageState = XMessage.MessageState.OPTED_IN;
            } else if (message.getType().equals("OPT_OUT")) {
                xmsgPayload.setText("stop-wa");
                messageState = XMessage.MessageState.OPTED_OUT;
            } else {
                messageState = XMessage.MessageState.REPLIED;
                xmsgPayload.setText(message.getText());
                messageIdentifier.setChannelMessageId(message.getMessageId());
            }
            List<XMessageDAO> msg1 = xmsgRepo.findAllByUserIdOrderByTimestamp(from.getUserID());
            if (msg1.size() > 0) {
                XMessageDAO msg0 = msg1.get(0);
                lastMsgId = msg0.getId();
            }
        }
        return XMessage.builder()
                .app(appName)
                .to(to)
                .from(from)
                .channelURI("WhatsApp")
                .providerURI("gupshup")
                .messageState(messageState)
                .messageId(messageIdentifier)
                .timestamp(message.getTimestamp() == null ? Timestamp.valueOf(LocalDateTime.now()).getTime() : message.getTimestamp())
                .payload(xmsgPayload)
                .lastMessageID(String.valueOf(lastMsgId)).build();
    }

    /**
     * @param from: User form the whom the message was received.
     * @param text: User's text
     * @return appName
     */
    private String getAppName(SenderReceiverInfo from, String text) {
        String appName;
        try {
            appName = (String) CampaignService.getCampaignFromStartingMessage(text).data.get("appName");
            return appName;
        } catch (Exception e) {
            XMessageDAO xMessageLast = xmsgRepo.findTopByUserIdAndMessageStateOrderByTimestampDesc(from.getUserID(), "REPLIED");
            return xMessageLast.getApp();
        }
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
                "        <text>Hi! Welcome . SamagraBot welcomes you!\n" +
                "\n" +
                "I will help you with different Org Workflows. During the flow, Press # to go to the previous step and * to go back to the original menu. \n" +
                "__ \n" +
                "\n" +
                "Please select the number corresponding to the option you want to proceed ahead with. \n" +
                "__ \n" +
                "\n" +
                "1 Leave Balance\n" +
                "2 Leave Application\n" +
                "3 Air Ticket\n" +
                "4 Train Ticket</text>\n" +
                "    </payload>\n" +
                "    <provider>gupshup</provider>\n" +
                "    <providerURI>gupshup</providerURI>\n" +
                "    <timestamp>1597777963000</timestamp>\n" +
                "    <to>\n" +
                "        <bot>false</bot>\n" +
                "        <broadcast>false</broadcast>\n" +
                "        <userID>9718908699</userID>\n" +
                "    </to>\n" +
                "</xMessage>";
        XMessage currentXmsg = XMessageParser.parse(new ByteArrayInputStream(x.getBytes()));
        adapter.callOutBoundAPI(currentXmsg);
    }

    public XMessage callOutBoundAPI(XMessage xMsg) throws Exception {
        log.info("next question to user is {}", new ObjectMapper().writeValueAsString(xMsg));

        // UAT credentials
        /*

        String passwordHSM = "SvKg3U74";
        String usernameHSM = "2000193031";

        String password2Way = "v@MPj33Q";
        String username2Way = "2000193033";
         */

        // Prod credentials

        String passwordHSM = "H8SPeFbJ";
        String usernameHSM = "2000193032";

        String password2Way = "JyVG5#a!";
        String username2Way = "2000193034";

        UriComponentsBuilder builder = UriComponentsBuilder.fromHttpUrl(GUPSHUP_OUTBOUND).
                queryParam("v", "1.1").
                queryParam("format", "json").
                queryParam("auth_scheme", "plain").
                queryParam("extra", "Samagra").
                queryParam("data_encoding", "text").
                queryParam("messageId", "123456789");
        if (xMsg.getMessageState().equals(XMessage.MessageState.OPTED_IN)) {
            builder.queryParam("channel", xMsg.getChannelURI().toLowerCase()).
                    queryParam("userid", username2Way).
                    queryParam("password", password2Way).
                    queryParam("phone_number", "91" + xMsg.getTo().getUserID()).
                    queryParam("method", "OPT_IN");
        }  else if (xMsg.getMessageType() != null && xMsg.getMessageType().equals(XMessage.MessageType.HSM)){
            optInUser(xMsg);

            builder.queryParam("method", "SendMessage").
                    queryParam("userid", usernameHSM).
                    queryParam("password", passwordHSM).
                    queryParam("send_to", "91" + xMsg.getTo().getUserID()).
                    queryParam("msg", xMsg.getPayload().getText()).
                    queryParam("isHSM", true).
                    queryParam("msg_type", "HSM");
        }else if (xMsg.getMessageState().equals(XMessage.MessageState.REPLIED)) {
            System.out.println(xMsg.getPayload().getText());
            builder.queryParam("method", "SendMessage").
                    queryParam("userid", username2Way).
                    queryParam("password", password2Way).
                    queryParam("send_to", "91" + xMsg.getTo().getUserID()).
                    queryParam("msg", xMsg.getPayload().getText()).
                    queryParam("msg_type", "TEXT");
        }else{}

        URI expanded = URI.create(builder.toUriString());
        RestTemplate restTemplate = new RestTemplate();
        GSWhatsappOutBoundResponse response = restTemplate.getForObject(expanded, GSWhatsappOutBoundResponse.class);
        log.info("response ================{}", new ObjectMapper().writeValueAsString(response));
        xMsg.setMessageId(MessageId.builder().channelMessageId(response.getResponse().getId()).build());

        XMessageDAO dao = XMessageDAOUtills.convertXMessageToDAO(xMsg);
        xmsgRepo.save(dao);
        return xMsg;
    }

    private void optInUser(XMessage xMsg) {
        UriComponentsBuilder optInBuilder = UriComponentsBuilder.fromHttpUrl(GUPSHUP_OUTBOUND).
                queryParam("v", "1.1").
                queryParam("format", "json").
                queryParam("auth_scheme", "plain").
                queryParam("method", "OPT_IN").
                queryParam("userid", "2000193031").
                queryParam("password", "SvKg3U74").
                queryParam("channel", "WHATSAPP").
                queryParam("phone_number", "91" + xMsg.getTo().getUserID()).
                queryParam("messageId", "123456789");

        URI expanded = URI.create(optInBuilder.toUriString());
        System.out.println(expanded.toString());
        RestTemplate restTemplate = new RestTemplate();
        String result = restTemplate.getForObject(expanded, String.class);
        System.out.println(result);
    }

    public HttpHeaders getVerifyHttpHeader() throws Exception {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);
        headers.add("Cache-Control", "no-cache");
        headers.add("apikey", gsApiKey);
        return headers;
    }
}
