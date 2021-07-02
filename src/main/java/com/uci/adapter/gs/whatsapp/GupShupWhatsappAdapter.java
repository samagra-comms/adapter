package com.uci.adapter.gs.whatsapp;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.uci.adapter.provider.factory.AbstractProvider;
import com.uci.adapter.provider.factory.IProvider;
import com.uci.dao.models.XMessageDAO;
import com.uci.dao.repository.XMessageRepository;
import com.uci.dao.utils.XMessageDAOUtills;
import com.uci.utils.BotService;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import messagerosa.core.model.MessageId;
import messagerosa.core.model.SenderReceiverInfo;
import messagerosa.core.model.XMessage;
import messagerosa.core.model.XMessagePayload;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;
import reactor.core.publisher.Mono;

import java.net.URI;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.*;
import java.util.function.Function;

@Getter
@Setter
class GWCredentials {
    String passwordHSM;
    String usernameHSM;
    String password2Way;
    String username2Way;
}

@Slf4j
@Getter
@Setter
@Builder
public class GupShupWhatsappAdapter extends AbstractProvider implements IProvider {

    @Value("${provider.gupshup.whatsapp.apikey}")
    private String gsApiKey;

    private final static String GUPSHUP_OUTBOUND = "https://media.smsgupshup.com/GatewayAPI/rest";
    @Autowired
    @Qualifier("rest")
    private RestTemplate restTemplate;

    private BotService botservice;

    public XMessageRepository xmsgRepo;

    @Override
    public Mono<XMessage> convertMessageToXMsg(Object msg) throws JsonProcessingException {
        GSWhatsAppMessage message = (GSWhatsAppMessage) msg;
        SenderReceiverInfo from = SenderReceiverInfo.builder().build();
        SenderReceiverInfo to = SenderReceiverInfo.builder().userID("admin").build();

        final XMessage.MessageState[] messageState = new XMessage.MessageState[1];
        messageState[0] = XMessage.MessageState.REPLIED;
        MessageId messageIdentifier = MessageId.builder().build();

        XMessagePayload xmsgPayload = XMessagePayload.builder().build();
        String appName = "";
        final String[] adapter = {""};

        log.info("test");

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
                switch (eventType) {
                    case "SENT":
                        messageState[0] = XMessage.MessageState.SENT;
                        break;
                    case "DELIVERED":
                        messageState[0] = XMessage.MessageState.DELIVERED;
                        break;
                    case "READ":
                        messageState[0] = XMessage.MessageState.READ;
                        break;
                    default:
                        messageState[0] = XMessage.MessageState.FAILED_TO_DELIVER;
                        //TODO: Save the state of message and reason in this case.
                        break;
                }
            }
            return getAppName(from, message.getText()).flatMap(new Function<String, Mono<? extends XMessage>>() {
                @Override
                public Mono<XMessage> apply(String a) {
                    if (a == null || a.isEmpty()) {
                        return Mono.just(processedXMessage(message, xmsgPayload, a, to, from, "",
                                messageState[0], messageIdentifier));
                    } else {
                        return botservice.getCurrentAdapter(a).map(new Function<String, XMessage>() {
                            @Override
                            public XMessage apply(String frc) {
                                return processedXMessage(message, xmsgPayload, a, to, from, frc,
                                        messageState[0], messageIdentifier);
                            }
                        });
                    }
                }
            });
        }

        else if (message.getType().equals("text")) {
            //Actual Message with payload (user response)
            from.setUserID(message.getMobile().substring(2));
            return getAppName(from, message.getText()).flatMap(new Function<String, Mono<? extends XMessage>>() {
                @Override
                public Mono<XMessage> apply(String appName) {
                    return botservice.getCurrentAdapter(appName).map(new Function<String, XMessage>() {
                        @Override
                        public XMessage apply(String adapterName) {
                            messageIdentifier.setReplyId(message.getReplyId());
                            if (message.getType().equals("OPT_IN")) {
                                messageState[0] = XMessage.MessageState.OPTED_IN;
                            } else if (message.getType().equals("OPT_OUT")) {
                                xmsgPayload.setText("stop-wa");
                                messageState[0] = XMessage.MessageState.OPTED_OUT;
                            } else {
                                messageState[0] = XMessage.MessageState.REPLIED;
                                xmsgPayload.setText(message.getText());
                                messageIdentifier.setChannelMessageId(message.getMessageId());
                            }

                            return processedXMessage(message, xmsgPayload, appName, to, from, adapterName,
                                    messageState[0], messageIdentifier);
                        }
                    });
                }
            });
        } else if (message.getType().equals("button")) {
            from.setUserID(message.getMobile().substring(2));
            // Get the last message sent to this user using the reply-messageID
            // Get the app from that message
            // Get the buttonLinkedApp
            // Add the starting text as the first message.

//            XMessageDAO lastMessage = xmsgRepo.findTopByUserIdAndMessageStateOrderByTimestampDesc(from.getUserID(), "SENT");
//             appName = lastMessage.getApp();
//            Application application = botservice.getButtonLinkedApp(appName);
//            appName = application.name;
//            xmsgPayload.setText((String) application.data.get("startingMessage"));
            return Mono.just(processedXMessage(message,xmsgPayload,appName,to,from, adapter[0], messageState[0],messageIdentifier));
        }
        return null;

    }

    private XMessage processedXMessage(GSWhatsAppMessage message, XMessagePayload xmsgPayload,
                                       String appName, SenderReceiverInfo to, SenderReceiverInfo from,
                                       String adapter, XMessage.MessageState messageState, MessageId messageIdentifier) {
        if (message.getLocation() != null) xmsgPayload.setText(message.getLocation());
        return XMessage.builder()
                .app(appName)
                .to(to)
                .from(from)
                .adapterId(adapter)
                .channelURI("WhatsApp")
                .providerURI("gupshup")
                .messageState(messageState)
                .messageId(messageIdentifier)
                .timestamp(message.getTimestamp() == null ? Timestamp.valueOf(LocalDateTime.now()).getTime() : message.getTimestamp())
                .payload(xmsgPayload).build();
    }

    /**
     * @param from: User form the whom the message was received.
     * @param text: User's text
     * @return appName
     */
    private Mono<String> getAppName(SenderReceiverInfo from, String text) {
        try {
            return botservice.getCampaignFromStartingMessage(text).map(new Function<String, String>() {
                @Override
                public String apply(String appName) {
                    if (appName == null) {
                        appName = "";
//                        try {
//                            XMessageDAO xMessageLast = xmsgRepo.findTopByUserIdAndMessageStateOrderByTimestampDesc(from.getUserID(), "SENT");
//                            appName = xMessageLast.getApp();
//                        } catch (Exception e2) {
//                            XMessageDAO xMessageLast = xmsgRepo.findTopByUserIdAndMessageStateOrderByTimestampDesc(from.getUserID(), "SENT");
//                            appName = xMessageLast.getApp();
//                        }
                    }
                    return appName;
                }
            });

        } catch (Exception e) {
            String appName="";
//            try {
//                XMessageDAO xMessageLast = xmsgRepo.findTopByUserIdAndMessageStateOrderByTimestampDesc(from.getUserID(), "SENT");
//                appName = xMessageLast.getApp();
//            } catch (Exception e2) {
//                XMessageDAO xMessageLast = xmsgRepo.findTopByUserIdAndMessageStateOrderByTimestampDesc(from.getUserID(), "SENT");
//                appName = xMessageLast.getApp();
//            }
            return Mono.justOrEmpty(appName);
        }
    }

    @Override
    public void processOutBoundMessage(XMessage nextMsg) throws Exception {
        log.info("nextXmsg {}", nextMsg.toXML());
        callOutBoundAPI(nextMsg);
    }

    @Override
    public Mono<XMessage> processOutBoundMessageF(XMessage nextMsg) throws Exception {
        return null;
    }

    public XMessage callOutBoundAPI(XMessage xMsg) throws Exception {
        log.info("next question to user is {}", xMsg.toXML());
        String url = botservice.CAMPAIGN_URL + "admin/v1/adapter/getCredentials/" + xMsg.getAdapterId();
        GWCredentials credentials = restTemplate.getForObject(url, GWCredentials.class);

        UriComponentsBuilder builder = UriComponentsBuilder.fromHttpUrl(GUPSHUP_OUTBOUND).
                queryParam("v", "1.1").
                queryParam("format", "json").
                queryParam("auth_scheme", "plain").
                queryParam("extra", "Samagra").
                queryParam("data_encoding", "text").
                queryParam("messageId", "123456789");
        if (xMsg.getMessageState().equals(XMessage.MessageState.OPTED_IN)) {
            builder.queryParam("channel", xMsg.getChannelURI().toLowerCase()).
                    queryParam("userid", credentials.username2Way).
                    queryParam("password", credentials.password2Way).
                    queryParam("phone_number", "91" + xMsg.getTo().getUserID()).
                    queryParam("method", "OPT_IN");
        } else if (xMsg.getMessageType() != null && xMsg.getMessageType().equals(XMessage.MessageType.HSM)) {
            optInUser(xMsg, credentials.usernameHSM, credentials.passwordHSM, credentials.username2Way, credentials.password2Way);

            builder.queryParam("method", "SendMessage").
                    queryParam("userid", credentials.usernameHSM).
                    queryParam("password", credentials.passwordHSM).
                    queryParam("send_to", "91" + xMsg.getTo().getUserID()).
                    queryParam("msg", xMsg.getPayload().getText()).
                    queryParam("isHSM", true).
                    queryParam("msg_type", "HSM");
        } else if (xMsg.getMessageType() != null && xMsg.getMessageType().equals(XMessage.MessageType.HSM_WITH_BUTTON)) {
            optInUser(xMsg, credentials.usernameHSM, credentials.passwordHSM, credentials.username2Way, credentials.password2Way);

            builder.queryParam("method", "SendMessage").
                    queryParam("userid", credentials.usernameHSM).
                    queryParam("password", credentials.passwordHSM).
                    queryParam("send_to", "91" + xMsg.getTo().getUserID()).
                    queryParam("msg", xMsg.getPayload().getText()).
                    queryParam("isTemplate", "true").
                    queryParam("msg_type", "HSM");
        } else if (xMsg.getMessageState().equals(XMessage.MessageState.REPLIED)) {
            System.out.println(xMsg.getPayload().getText());
            builder.queryParam("method", "SendMessage").
                    queryParam("userid", credentials.username2Way).
                    queryParam("password", credentials.password2Way).
                    queryParam("send_to", "91" + xMsg.getTo().getUserID()).
                    queryParam("msg", xMsg.getPayload().getText()).
                    queryParam("msg_type", "TEXT");
        } else {
        }
        log.info(xMsg.getPayload().getText());
        URI expanded = URI.create(builder.toUriString());
        log.info(expanded.toString());
        RestTemplate restTemplate = new RestTemplate();
        GSWhatsappOutBoundResponse response = restTemplate.getForObject(expanded, GSWhatsappOutBoundResponse.class);
        log.info("response ================{}", new ObjectMapper().writeValueAsString(response));
        xMsg.setMessageId(MessageId.builder().channelMessageId(response.getResponse().getId()).build());
        xMsg.setMessageState(XMessage.MessageState.SENT);

        XMessageDAO dao = XMessageDAOUtills.convertXMessageToDAO(xMsg);
        xmsgRepo.save(dao);
        return xMsg;
    }

    private void optInUser(XMessage xMsg, String usernameHSM, String passwordHSM, String username2Way, String password2Way) {
        UriComponentsBuilder optInBuilder = UriComponentsBuilder.fromHttpUrl(GUPSHUP_OUTBOUND).
                queryParam("v", "1.1").
                queryParam("format", "json").
                queryParam("auth_scheme", "plain").
                queryParam("method", "OPT_IN").
                queryParam("userid", usernameHSM).
                queryParam("password", passwordHSM).
                queryParam("channel", "WHATSAPP").
                queryParam("phone_number", "91" + xMsg.getTo().getUserID()).
                queryParam("messageId", "123456789");

        URI expanded = URI.create(optInBuilder.toUriString());
        System.out.println(expanded.toString());
        RestTemplate restTemplate = new RestTemplate();
        String result = restTemplate.getForObject(expanded, String.class);
        System.out.println(result);
    }
}