package com.uci.adapter.firebase.web;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.uci.adapter.firebase.web.inbound.FirebaseWebMessage;
import com.uci.adapter.provider.factory.AbstractProvider;
import com.uci.adapter.provider.factory.IProvider;
import com.uci.utils.BotService;
import com.uci.utils.service.VaultService;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import messagerosa.core.model.*;
import org.jetbrains.annotations.NotNull;
import org.springframework.beans.factory.annotation.Autowired;
import reactor.core.publisher.Mono;

import javax.xml.bind.JAXBException;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;

@Slf4j
@Getter
@Setter
@Builder
public class FirebaseNotificationAdapter extends AbstractProvider implements IProvider {
    @Autowired
    public BotService botService;

    @Autowired
    public VaultService vaultService;

    /**
     * Convert Firebase Message Object to XMessage Object
     * @param message
     * @return
     * @throws JAXBException
     * @throws JsonProcessingException
     */
    public Mono<XMessage> convertMessageToXMsg(Object message) throws JAXBException, JsonProcessingException {
        FirebaseWebMessage webMessage = (FirebaseWebMessage) message;
        SenderReceiverInfo from = SenderReceiverInfo.builder().deviceType(DeviceType.PHONE).build();
        SenderReceiverInfo to = SenderReceiverInfo.builder().userID("admin").build();
        XMessage.MessageState messageState = XMessage.MessageState.REPLIED;
        String eventType = webMessage.getEventType();
        /* Message state changed by event type */
        if(eventType != null && (eventType.equals("DELIVERED")
                || eventType.equals("READ"))) {
            messageState = getMessageState(eventType);
        }
        MessageId messageIdentifier = MessageId.builder().build();

        XMessagePayload xmsgPayload = XMessagePayload.builder().build();
        log.info("XMessage Payload getting created >>>");
        xmsgPayload.setText(webMessage.getText());
        XMessage.MessageType messageType= XMessage.MessageType.TEXT;
        //Todo: How to get Button choices from normal text
        from.setUserID(webMessage.getFrom());
        if(webMessage.getFcmToken() != null) {
            Map<String, String> meta = new HashMap();
            meta.put("fcmToken", webMessage.getFcmToken());
            from.setMeta(meta);
        }

        /* To use later in outbound reply message's message id & to */
        messageIdentifier.setChannelMessageId(webMessage.getMessageId());
        messageIdentifier.setReplyId(webMessage.getFrom());

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
        log.info("Current message :: " +  x.toString());
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
     * @param nextMsg
     * @return
     * @throws Exception
     */
    @Override
    public Mono<XMessage> processOutBoundMessageF(XMessage nextMsg) throws Exception {
        SenderReceiverInfo to = nextMsg.getTo();
        Map<String, String> meta = to.getMeta();
        if(meta != null && meta.get("fcmToken") != null) {
            return botService.getAdapterByID(nextMsg.getAdapterId()).map(new Function<JsonNode, Mono<Mono<XMessage>>>() {
                @Override
                public Mono<Mono<XMessage>> apply(JsonNode adapter) {
                    log.info("adapter: "+adapter);
                    if(adapter != null) {
                        String vaultKey;
                        try{
                            vaultKey = adapter.path("config").path("credentials").path("variable").asText();
                        } catch (Exception ex) {
                            log.error("Exception in fetching adapter variable from json node: "+ex.getMessage());
                            vaultKey = null;
                        }

                        if(vaultKey != null && !vaultKey.isEmpty()) {
                            return vaultService.getAdpaterCredentials(vaultKey).map(new Function<JsonNode, Mono<XMessage>>(){
                                @Override
                                public Mono<XMessage> apply(JsonNode credentials) {
                                    log.info("credentials: "+credentials);
                                    if(credentials != null && credentials.path("serviceKey") != null
                                            && !credentials.path("serviceKey").asText().isEmpty()) {
                                        return (new FirebaseNotificationService()).sendNotificationMessage(credentials.path("serviceKey").asText(), meta.get("fcmToken"), "Firebase Notification", nextMsg.getPayload().getText())
                                            .map(new Function<Boolean, XMessage>() {
                                                @Override
                                                public XMessage apply(Boolean result) {
                                                    if (result) {
                                                        nextMsg.setTo(to);
                                                        nextMsg.setMessageId(MessageId.builder().channelMessageId(LocalDateTime.now().toString()).build());
                                                        nextMsg.setMessageState(XMessage.MessageState.SENT);
                                                    }
                                                    return nextMsg;
                                                }
                                            });
                                    }
                                    return Mono.just(null);
                                }
                            });
                        }
                    }
                    return Mono.just(Mono.just(null));
                }
            }).flatMap(new Function<Mono<Mono<XMessage>>, Mono<XMessage>>() {
                @Override
                public Mono<XMessage> apply(Mono<Mono<XMessage>> m) {
                    log.info("Mono FlatMap Level 1");
                    return m.flatMap(new Function<Mono<XMessage>, Mono<? extends XMessage>>() {
                        @Override
                        public Mono<? extends XMessage> apply(Mono<XMessage> n) {
                            log.info("Mono FlatMap Level 2");
                            return n;
                        }
                    });
                }
            });

        }
        return null;
    }
}
