package com.uci.adapter.gs.whatsapp;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.uci.adapter.provider.factory.AbstractProvider;
import com.uci.adapter.provider.factory.IProvider;
import com.uci.dao.models.XMessageDAO;
import com.uci.dao.repository.XMessageRepository;
import com.uci.dao.utils.XMessageDAOUtils;
import com.uci.utils.BotService;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import messagerosa.core.model.ButtonChoice;
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
import java.util.function.Consumer;
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

    private String gsApiKey = "test";

    private final static String GUPSHUP_OUTBOUND = "https://media.smsgupshup.com/GatewayAPI/rest";
    @Autowired
    @Qualifier("rest")
    private RestTemplate restTemplate;

    @Autowired
    private BotService botservice;

    public XMessageRepository xmsgRepo;

    @Value("${campaign.url}")
    public String CAMPAIGN_URL;

    @Override
    public Mono<XMessage> convertMessageToXMsg(Object msg) throws JsonProcessingException {
        GSWhatsAppMessage message = (GSWhatsAppMessage) msg;
        SenderReceiverInfo from = SenderReceiverInfo.builder().build();
        SenderReceiverInfo to = SenderReceiverInfo.builder().userID("admin").build();

        final XMessage.MessageState[] messageState = new XMessage.MessageState[1];
        messageState[0] = XMessage.MessageState.REPLIED;
        MessageId messageIdentifier = MessageId.builder().build();
        XMessage.MessageType messageType= XMessage.MessageType.TEXT;
        XMessagePayload xmsgPayload = XMessagePayload.builder().build();
        String appName = "";
        final String[] adapter = {""};

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
            return Mono.just(processedXMessage(message, xmsgPayload, to, from, messageState[0], messageIdentifier, messageType));
        }

        else if (message.getType().equals("text")) {
            //Actual Message with payload (user response)
            from.setUserID(message.getMobile().substring(2));
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
            return Mono.just(processedXMessage(message, xmsgPayload, to, from, messageState[0], messageIdentifier, messageType));
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
            return Mono.just(processedXMessage(message, xmsgPayload, to, from, messageState[0],messageIdentifier, messageType));
        }
        return null;

    }

    private XMessage processedXMessage(GSWhatsAppMessage message, XMessagePayload xmsgPayload, SenderReceiverInfo to,
                                       SenderReceiverInfo from, XMessage.MessageState messageState,
                                       MessageId messageIdentifier, XMessage.MessageType messageType) {
        if (message.getLocation() != null) xmsgPayload.setText(message.getLocation());
        return XMessage.builder()
                .to(to)
                .from(from)
                .channelURI("WhatsApp")
                .providerURI("gupshup")
                .messageState(messageState)
                .messageId(messageIdentifier)
                .messageType(messageType)
                .timestamp(message.getTimestamp() == null ? Timestamp.valueOf(LocalDateTime.now()).getTime() : message.getTimestamp())
                .payload(xmsgPayload).build();
    }


    @Override
    public void processOutBoundMessage(XMessage nextMsg) throws Exception {
        log.info("nextXmsg {}", nextMsg.toXML());
        callOutBoundAPI(nextMsg);
    }

    @Override
    public Mono<XMessage> processOutBoundMessageF(XMessage nextMsg) throws Exception {
    	log.info("processOutBoundMessageF nextXmsg {}", nextMsg.toXML());
        return callOutBoundAPI(nextMsg);
    }

    public Mono<XMessage> callOutBoundAPI(XMessage xMsg) throws Exception {
        log.info("next question to user is {}", xMsg.toXML());
        return botservice.getGupshupAdpaterCredentials(xMsg.getAdapterId()).map(new Function<Map<String, String>, XMessage>() {
            @Override
            public XMessage apply(Map<String, String> credentials) {

                String text = xMsg.getPayload().getText() + renderMessageChoices(xMsg.getPayload().getButtonChoices());
                
            	UriComponentsBuilder builder = UriComponentsBuilder.fromHttpUrl(GUPSHUP_OUTBOUND).
                        queryParam("v", "1.1").
                        queryParam("format", "json").
                        queryParam("auth_scheme", "plain").
                        queryParam("extra", "Samagra").
                        queryParam("data_encoding", "text").
                        queryParam("messageId", "123456789");
                if (xMsg.getMessageState().equals(XMessage.MessageState.OPTED_IN)) {
                    builder.queryParam("channel", xMsg.getChannelURI().toLowerCase()).
                            queryParam("userid", credentials.get("username2Way")).
                            queryParam("password", credentials.get("password2Way")).
                            queryParam("phone_number", "91" + xMsg.getTo().getUserID()).
                            queryParam("method", "OPT_IN");
                } else if (xMsg.getMessageType() != null && xMsg.getMessageType().equals(XMessage.MessageType.HSM)) {
                    optInUser(xMsg, credentials.get("usernameHSM"), credentials.get("passwordHSM"), credentials.get("username2Way"), credentials.get("password2Way"));

                    builder.queryParam("method", "SendMessage").
                            queryParam("userid", credentials.get("usernameHSM")).
                            queryParam("password", credentials.get("passwordHSM")).
                            queryParam("send_to", "91" + xMsg.getTo().getUserID()).
                            queryParam("msg", text).
                            queryParam("isHSM", true).
                            queryParam("msg_type", "HSM");
                } else if (xMsg.getMessageType() != null && xMsg.getMessageType().equals(XMessage.MessageType.HSM_WITH_BUTTON)) {
                    optInUser(xMsg, credentials.get("usernameHSM"), credentials.get("passwordHSM"), credentials.get("username2Way"), credentials.get("password2Way"));

                    builder.queryParam("method", "SendMessage").
                            queryParam("userid", credentials.get("usernameHSM")).
                            queryParam("password", credentials.get("passwordHSM")).
                            queryParam("send_to", "91" + xMsg.getTo().getUserID()).
                            queryParam("msg", text).
                            queryParam("isTemplate", "true").
                            queryParam("msg_type", "HSM");
                } else if (xMsg.getMessageState().equals(XMessage.MessageState.REPLIED)) {
                    System.out.println(text);
                    builder.queryParam("method", "SendMessage").
                            queryParam("userid", credentials.get("username2Way")).
                            queryParam("password", credentials.get("password2Way")).
                            queryParam("send_to", "91" + xMsg.getTo().getUserID()).
                            queryParam("msg", text).
                            queryParam("msg_type", "TEXT");
                } else {
                }
                log.info(text);
                URI expanded = URI.create(builder.toUriString());
                log.info(expanded.toString());
                RestTemplate restTemplate = new RestTemplate();
                GSWhatsappOutBoundResponse response = restTemplate.getForObject(expanded, GSWhatsappOutBoundResponse.class);
                try {
					log.info("response ================{}", new ObjectMapper().writeValueAsString(response));
				} catch (JsonProcessingException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
					log.error("Error in callOutBoundAPI for objectmapper: "+e.getMessage());
				}
                xMsg.setMessageId(MessageId.builder().channelMessageId(response.getResponse().getId()).build());
                xMsg.setMessageState(XMessage.MessageState.SENT);

                XMessageDAO dao = XMessageDAOUtils.convertXMessageToDAO(xMsg);
                xmsgRepo.insert(dao);
                return xMsg;
            }
        });
    }
    
    private String renderMessageChoices(ArrayList<ButtonChoice> buttonChoices) {
        StringBuilder processedChoicesBuilder = new StringBuilder("");
        if(buttonChoices != null){
            for(ButtonChoice choice:buttonChoices){
                processedChoicesBuilder.append(choice.getText()).append("\n");
            }
            String processedChoices = processedChoicesBuilder.toString();
            return processedChoices.substring(0,processedChoices.length()-1);
        }
        return "";
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