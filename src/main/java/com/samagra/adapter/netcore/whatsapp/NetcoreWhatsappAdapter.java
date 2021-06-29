package com.samagra.adapter.netcore.whatsapp;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.samagra.adapter.netcore.whatsapp.inbound.NetcoreWhatsAppMessage;
import com.samagra.adapter.netcore.whatsapp.outbound.OutboundMessage;
import com.samagra.adapter.netcore.whatsapp.outbound.SendMessageResponse;
import com.samagra.adapter.netcore.whatsapp.outbound.SingleMessage;
import com.samagra.adapter.netcore.whatsapp.outbound.Text;
import com.samagra.adapter.provider.factory.AbstractProvider;
import com.samagra.adapter.provider.factory.IProvider;
import com.uci.utils.BotService;
import io.fusionauth.domain.Application;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import messagerosa.core.model.*;
import messagerosa.dao.XMessageDAO;
import messagerosa.dao.XMessageDAOUtills;
import messagerosa.dao.XMessageRepo;
import org.jetbrains.annotations.NotNull;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.web.client.RestTemplate;
import reactor.core.publisher.Mono;

import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;

@Slf4j
@Getter
@Setter
@Builder
public class NetcoreWhatsappAdapter extends AbstractProvider implements IProvider {

    private final static String GUPSHUP_OUTBOUND = "https://media.smsgupshup.com/GatewayAPI/rest";
    @Autowired
    @Qualifier("rest")
    private RestTemplate restTemplate;

    private BotService botservice;

    public XMessageRepo xmsgRepo;

    @Override
    public Mono<XMessage> convertMessageToXMsg(Object msg) throws JsonProcessingException {
        NetcoreWhatsAppMessage message = (NetcoreWhatsAppMessage) msg;
        SenderReceiverInfo from = SenderReceiverInfo.builder().build();
        SenderReceiverInfo to = SenderReceiverInfo.builder().userID("admin").build();

        XMessage.MessageState messageState;
        messageState = XMessage.MessageState.REPLIED;
        MessageId messageIdentifier = MessageId.builder().build();

        XMessagePayload xmsgPayload = XMessagePayload.builder().build();
        final long[] lastMsgId = {0};
        String appName = "";
        final String[] adapter = {""};

        log.info("test");

        if (message.getEventType() != null) {
            xmsgPayload.setText("");
            messageIdentifier.setChannelMessageId(message.getMessageId());
            from.setUserID(message.getMobile().substring(2));
            return getAppName(from, "").flatMap(new Function<String, Mono<? extends XMessage>>() {
                @Override
                public Mono<XMessage> apply(String a) {
                    return botservice.getCurrentAdapter(a).map(new Function<String, XMessage>() {
                        @Override
                        public XMessage apply(String frc) {
                            adapter[0] = frc;
                            XMessage.MessageState messageState1;
                            String eventType = message.getEventType().toUpperCase();
                            messageState1 = getMessageState(eventType);
                            return processedXMessage(message, xmsgPayload, to, from, adapter[0], messageState1, messageIdentifier, lastMsgId[0], a);
                        }
                    });
                }
            });

        } else if (message.getType().equalsIgnoreCase("text")) {
            //Actual Message with payload (user response)
            messageState = XMessage.MessageState.REPLIED;
            from.setUserID(message.getMobile().substring(2));

            XMessage.MessageState finalMessageState = messageState;
            return getAppName(from, message.getText().getText()).flatMap(new Function<String, Mono<? extends XMessage>>() {
                @Override
                public Mono<XMessage> apply(String botName) {
                  return botservice.getCurrentAdapter(botName).map(new Function<String, XMessage>() {
                       @Override
                       public XMessage apply(String adapterName) {

                           messageIdentifier.setReplyId(message.getReplyId());
                           xmsgPayload.setText(message.getText().getText());

                           messageIdentifier.setChannelMessageId(message.getMessageId());
                           List<XMessageDAO> msg1 =  xmsgRepo.findAllByUserIdOrderByTimestamp(from.getUserID());
                           if (msg1.size() > 0) {
                               XMessageDAO msg0 = msg1.get(0);
                               lastMsgId[0] = msg0.getId();
                           }
                           return processedXMessage(message, xmsgPayload, to, from, adapterName, finalMessageState, messageIdentifier, lastMsgId[0], botName);
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

            XMessageDAO lastMessage = xmsgRepo.findTopByUserIdAndMessageStateOrderByTimestampDesc(from.getUserID(), "SENT");
            lastMsgId[0] = lastMessage.getId();
            appName = lastMessage.getApp();
            Application application = botservice.getButtonLinkedApp(appName);
            appName = application.name;
            xmsgPayload.setText((String) application.data.get("startingMessage"));
            return Mono.just(processedXMessage(message, xmsgPayload, to, from, adapter[0], messageState, messageIdentifier, lastMsgId[0], appName));

        } else {
            System.out.println("No Match for parsing");
            return Mono.just(processedXMessage(message, xmsgPayload, to, from, adapter[0], messageState, messageIdentifier, lastMsgId[0], appName));

        }

    }

    @NotNull
    public static XMessage.MessageState getMessageState(String eventType) {
        XMessage.MessageState messageState;
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
                //TODO: Save the state of message and reason in this case.
                break;
        }
        return messageState;
    }

    private XMessage processedXMessage(NetcoreWhatsAppMessage message, XMessagePayload xmsgPayload, SenderReceiverInfo to,
                                       SenderReceiverInfo from, String adapter, XMessage.MessageState messageState,
                                       MessageId messageIdentifier, long lastMsgId, String appName) {
        if (message.getLocation() != null) xmsgPayload.setText(message.getLocation());
        return XMessage.builder()
                .app(appName)
                .to(to)
                .from(from)
                .adapterId(adapter)
                .channelURI("WhatsApp")
                .providerURI("Netcore")
                .messageState(messageState)
                .messageId(messageIdentifier)
                .timestamp(getTimestamp(message.getEventType(), message.getTimestamp()))
                .payload(xmsgPayload)
                .lastMessageID(String.valueOf(lastMsgId)).build();
    }

    Long getTimestamp(String eventType, String timestamp) {
        return timestamp == null ? Timestamp.valueOf(LocalDateTime.now()).getTime() : Long.parseLong(timestamp) * 1000;
//        if (eventType != null)
//            return timestamp == null ? Timestamp.valueOf(LocalDateTime.now()).getTime() : Long.parseLong(timestamp)*1000;
//        else{
//            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
//            LocalDateTime date = LocalDateTime.parse(timestamp, formatter);
//            return Timestamp.valueOf(date).getTime();
//        }
    }

    /**
     * @param from: User form the whom the message was received.
     * @param text: User's text
     * @return appName
     */
    private Mono<String> getAppName(SenderReceiverInfo from, String text) {
        String appName;
        try {
            return botservice.getCampaignFromStartingMessage(text).map(appName1 -> {
                if (appName1 == null || appName1.equals("")) {
                    try {
                        XMessageDAO xMessageLast = xmsgRepo.findTopByUserIdAndMessageStateOrderByTimestampDesc(from.getUserID(), "SENT");
                        appName1 = xMessageLast.getApp();
                    } catch (Exception e2) {
                        XMessageDAO xMessageLast = xmsgRepo.findTopByUserIdAndMessageStateOrderByTimestampDesc(from.getUserID(), "SENT");
                        appName1 = xMessageLast.getApp();
                    }
                }
                return appName1;
            });
        } catch (Exception e) {
            try {
                XMessageDAO xMessageLast = xmsgRepo.findTopByUserIdAndMessageStateOrderByTimestampDesc(from.getUserID(), "SENT");
                appName = xMessageLast.getApp();
            } catch (Exception e2) {
                XMessageDAO xMessageLast = xmsgRepo.findTopByUserIdAndMessageStateOrderByTimestampDesc(from.getUserID(), "SENT");
                appName = xMessageLast.getApp();
            }
            return Mono.just(appName);
        }

    }

    @Override
    public void processOutBoundMessage(XMessage nextMsg) throws Exception {
        log.info("nextXmsg {}", nextMsg.toXML());
        callOutBoundAPI(nextMsg);
    }

    @Override
    public Mono<Boolean> processOutBoundMessageF(XMessage xMsg) {
        String token = "eyJ0eXAiOiJKV1QiLCJhbGciOiJIUzUxMiJ9.eyJzdWIiOiJuZXRjb3Jlc2FsZXNleHAiLCJleHAiOjI0MjUxMDI1MjZ9.ljC4Tvgz031i6DsKr2ILgCJsc9C_hxdo2Kw8iZp9tsVcCaKbIOXaFoXmpU7Yo7ob4P6fBtNtdNBQv_NSMq_Q8w";
        String phoneNo = "";
        String text = "";

//        phoneNo = "91" + xMsg.getTo().getUserID();

//        if (xMsg.getMessageState().equals(XMessage.MessageState.OPTED_IN)) {
//            //no implementation
//        } else
        if (xMsg.getMessageType() != null && xMsg.getMessageType().equals(XMessage.MessageType.HSM)) {
            // OPT in user
            text = xMsg.getPayload().getText() + renderMessageChoices(xMsg.getPayload().getButtonChoices());
        } else if (xMsg.getMessageType() != null && xMsg.getMessageType().equals(XMessage.MessageType.HSM_WITH_BUTTON)) {
            // OPT in user
            text = xMsg.getPayload().getText()+ renderMessageChoices(xMsg.getPayload().getButtonChoices());
        } else if (xMsg.getMessageState().equals(XMessage.MessageState.REPLIED)) {
            text = xMsg.getPayload().getText()+ renderMessageChoices(xMsg.getPayload().getButtonChoices());
        }
//            else {
//            //no implementation
//        }

        // SendMessage
        Text t = Text.builder().content(text).previewURL("false").build();
        Text[] texts = {t};
        return NewNetcoreService.getInstance(new NWCredentials(token)).
                sendOutboundMessage(OutboundMessage.builder().message(new SingleMessage[]{SingleMessage
                        .builder()
                        .from("461089f9-1000-4211-b182-c7f0291f3d45")
                        .to(phoneNo)
                        .recipientType("individual")
                        .messageType("text")
                        .header("custom_data")
                        .text(texts)
                        .build()}).build());


    }

    private String renderMessageChoices(ArrayList<ButtonChoice> buttonChoices) {
        StringBuilder processedChoicesBuilder = new StringBuilder("\n");
        if(buttonChoices != null){
            for(ButtonChoice choice:buttonChoices){
                processedChoicesBuilder.append(choice.getText()).append("\n");
            }
            String processedChoices = processedChoicesBuilder.toString();
            return processedChoices.substring(0,processedChoices.length()-1);
        }
        return "";
    }

    public XMessage callOutBoundAPI(XMessage xMsg) throws Exception {
        log.info("next question to user is {}", xMsg.toXML());
        // String url = "http://federation-service:9999/admin/v1/adapter/getCredentials/" + xMsg.getAdapterId();
        // NWCredentials credentials = restTemplate.getForObject(url, NWCredentials.class);

        String phoneNo = "";
        String text = "";

        phoneNo = "91" + xMsg.getTo().getUserID();

        if (xMsg.getMessageState().equals(XMessage.MessageState.OPTED_IN)) {

        } else if (xMsg.getMessageType() != null && xMsg.getMessageType().equals(XMessage.MessageType.HSM)) {
            // OPT in user
            text = xMsg.getPayload().getText()+ renderMessageChoices(xMsg.getPayload().getButtonChoices());;
        } else if (xMsg.getMessageType() != null && xMsg.getMessageType().equals(XMessage.MessageType.HSM_WITH_BUTTON)) {
            // OPT in user
            text = xMsg.getPayload().getText()+ renderMessageChoices(xMsg.getPayload().getButtonChoices());;
        } else if (xMsg.getMessageState().equals(XMessage.MessageState.REPLIED)) {
            text = xMsg.getPayload().getText()+ renderMessageChoices(xMsg.getPayload().getButtonChoices());;
        } else {
        }

        // SendMessage
        Text t = Text.builder().content(text).previewURL("false").build();
        Text[] texts = {t};

        SingleMessage msg = SingleMessage
                .builder()
                .from("461089f9-1000-4211-b182-c7f0291f3d45")
                .to(phoneNo)
                .recipientType("individual")
                .messageType("text")
                .header("custom_data")
                .text(texts)
                .build();
        SingleMessage[] messages = {msg};

        String token = "eyJ0eXAiOiJKV1QiLCJhbGciOiJIUzUxMiJ9.eyJzdWIiOiJuZXRjb3Jlc2FsZXNleHAiLCJleHAiOjI0MjUxMDI1MjZ9.ljC4Tvgz031i6DsKr2ILgCJsc9C_hxdo2Kw8iZp9tsVcCaKbIOXaFoXmpU7Yo7ob4P6fBtNtdNBQv_NSMq_Q8w";
        NWCredentials nc = NWCredentials.builder().build();
        nc.setToken(token);
        NetcoreService ns = new NetcoreService(nc);

        OutboundMessage outboundMessage = OutboundMessage.builder().message(messages).build();
        SendMessageResponse response = ns.sendText(outboundMessage);

        xMsg.setMessageId(MessageId.builder().channelMessageId(response.getData().getIdentifier()).build());
        xMsg.setMessageState(XMessage.MessageState.SENT);

        XMessageDAO dao = XMessageDAOUtills.convertXMessageToDAO(xMsg);
        xmsgRepo.save(dao);
        return xMsg;
    }

}