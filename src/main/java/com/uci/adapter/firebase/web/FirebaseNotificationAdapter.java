package com.uci.adapter.firebase.web;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.uci.adapter.firebase.web.inbound.FirebaseWebMessage;
import com.uci.adapter.firebase.web.inbound.FirebaseWebReport;
import com.uci.adapter.provider.factory.AbstractProvider;
import com.uci.adapter.provider.factory.IProvider;
import com.uci.utils.BotService;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import messagerosa.core.model.*;
import org.jetbrains.annotations.NotNull;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import reactor.core.publisher.Mono;

import javax.xml.bind.JAXBException;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;

@Slf4j
@Getter
@Setter
@Builder
public class FirebaseNotificationAdapter extends AbstractProvider implements IProvider {
    @Autowired
    public BotService botService;

    private String notificationKeyEnable;

    /**
     * Convert Firebase Message Object to XMessage Object
     *
     * @param message
     * @return
     * @throws JAXBException
     * @throws JsonProcessingException
     */
    public Mono<XMessage> convertMessageToXMsg(Object message) throws JAXBException, JsonProcessingException {
        FirebaseWebMessage webMessage = (FirebaseWebMessage) message;
        SenderReceiverInfo from = SenderReceiverInfo.builder().deviceType(DeviceType.PHONE_FCM).build();
        SenderReceiverInfo to = SenderReceiverInfo.builder().userID("admin").build();
        XMessage.MessageState messageState = XMessage.MessageState.REPLIED;
        MessageId messageIdentifier = MessageId.builder().build();
        String eventType = webMessage.getEventType();

        XMessagePayload xmsgPayload = XMessagePayload.builder().build();
        xmsgPayload.setText(webMessage.getText());
        XMessage.MessageType messageType = XMessage.MessageType.TEXT;

        /* To use later in outbound reply message's message id & to */
        /* Message state changed by event type */
        if (eventType != null && (eventType.equals("DELIVERED")
                || eventType.equals("READ")) && webMessage.getReport() != null) {
            FirebaseWebReport reportMsg = webMessage.getReport();
            messageState = getMessageState(eventType);
            messageIdentifier.setChannelMessageId(reportMsg.getExternalId());
            from.setUserID(reportMsg.getDestAdd());
            if (reportMsg.getFcmDestAdd() != null) {
                Map<String, String> meta = new HashMap();
                meta.put("fcmToken", reportMsg.getFcmDestAdd());
                from.setMeta(meta);
            }
        } else {
            messageIdentifier.setChannelMessageId(webMessage.getMessageId());
            messageIdentifier.setReplyId(webMessage.getFrom());
            from.setUserID(webMessage.getFrom());
        }

        XMessage x = XMessage.builder()
                .to(to)
                .from(from)
                .channelURI("web")
                .providerURI("firebase")
                .messageState(messageState)
                .messageId(messageIdentifier)
                .messageType(messageType)
                .timestamp(Timestamp.valueOf(LocalDateTime.now()).getTime())
                .payload(xmsgPayload).build();
        log.info("Current message :: " + x.toString());
        return Mono.just(x);
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

    /**
     * Process XMessage & send firebase notification
     *
     * @param nextMsg
     * @return
     * @throws Exception
     */
    @Override
    public Mono<XMessage> processOutBoundMessageF(XMessage nextMsg) {
        try {
            SenderReceiverInfo to = nextMsg.getTo();
            XMessagePayload payload = nextMsg.getPayload();
            Map<String, String> data = new HashMap<>();
            for (Data dataArrayList : payload.getData()) {
                data.put(dataArrayList.getKey(), dataArrayList.getValue());
            }
            if (data != null && data.get("fcmToken") != null) {
                return botService.getAdapterCredentials(nextMsg.getAdapterId()).map(new Function<JsonNode, Mono<XMessage>>() {
                    @Override
                    public Mono<XMessage> apply(JsonNode credentials) {
                        String channelMessageId = UUID.randomUUID().toString();
                        log.info("credentials: " + credentials);
                        if (credentials != null && credentials.path("serviceKey") != null
                                && !credentials.path("serviceKey").asText().isEmpty()) {
                            String click_action = null;
                            if (data.get("fcmClickActionUrl") != null && !data.get("fcmClickActionUrl").isEmpty()) {
                                click_action = data.get("fcmClickActionUrl");
                            }
                            return (new FirebaseNotificationService()).sendNotificationMessage(credentials.path("serviceKey").asText(), data.get("fcmToken"), nextMsg.getPayload().getTitle(), nextMsg.getPayload().getText(), click_action, nextMsg.getTo().getUserID(), channelMessageId, notificationKeyEnable, data)
                                    .map(new Function<Boolean, XMessage>() {
                                        @Override
                                        public XMessage apply(Boolean result) {
                                            if (result) {
                                                nextMsg.setTo(to);
                                                nextMsg.setMessageId(MessageId.builder().channelMessageId(channelMessageId).build());
                                                nextMsg.setMessageState(XMessage.MessageState.SENT);
                                            }
                                            return nextMsg;
                                        }
                                    });
                        }
                        return null;
                    }
                }).flatMap(new Function<Mono<XMessage>, Mono<? extends XMessage>>() {
                    @Override
                    public Mono<? extends XMessage> apply(Mono<XMessage> n) {
                        log.info("Mono FlatMap Level 2");
                        return n;
                    }
                });

            }
        } catch(Exception ex){
            log.error("FirebaseNotificationAdapter:processOutBoundMessageF::Exception: "+ex.getMessage());
        }
        return null;
    }
}
