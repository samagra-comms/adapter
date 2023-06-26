package com.uci.adapter.firebase.web;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.google.api.core.ApiFuture;
import com.google.api.core.ApiFutureCallback;
import com.google.api.core.ApiFutures;
import com.google.api.gax.rpc.ApiException;
import com.google.auth.oauth2.GoogleCredentials;
import com.google.common.util.concurrent.MoreExecutors;
import com.google.firebase.FirebaseApp;
import com.google.firebase.FirebaseOptions;
import com.google.firebase.messaging.*;
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
import org.reactivestreams.Publisher;
import org.springframework.beans.factory.annotation.Autowired;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import javax.xml.bind.JAXBException;
import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.function.Function;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

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
        } catch (Exception ex) {
            log.error("FirebaseNotificationAdapter:processOutBoundMessageF::Exception: " + ex.getMessage());
        }
        return null;
    }

    @Override
    public Flux<XMessage> processOutBoundMessageF(Mono<List<XMessage>> xMessageList) throws Exception {
        List<XMessage> xMessageListCass = new ArrayList<XMessage>();
        Map<String, List<Message>> adapterIdMap = new HashMap<>();
        Set<String> uniqueUserSet = new HashSet<>();
        final long startTime = System.nanoTime();
        return xMessageList
                .flatMapMany(Flux::fromIterable)
                .collectList()
                .map(new Function<List<XMessage>, Mono<List<XMessage>>>() {
                    public Mono<List<XMessage>> apply(List<XMessage> msgList) {
                        msgList.forEach(nextMsg -> {
                            SenderReceiverInfo to = nextMsg.getTo();
                            XMessagePayload payload = nextMsg.getPayload();
                            Map<String, String> data = new HashMap<>();
                            for (Data dataArrayList : payload.getData()) {
                                data.put(dataArrayList.getKey(), dataArrayList.getValue());
                            }
                            if (data != null && data.get("fcmToken") != null) {
                                String channelMessageId = UUID.randomUUID().toString();
                                String click_action = null;
                                if (data.get("fcmClickActionUrl") != null && !data.get("fcmClickActionUrl").isEmpty()) {
                                    click_action = data.get("fcmClickActionUrl");
                                }
                                Message message = Message.builder()
                                        .setNotification(Notification.builder()
                                                .setTitle(nextMsg.getPayload().getTitle())
                                                .setBody(nextMsg.getPayload().getText())
                                                .build())
                                        .setToken(data.get("fcmToken"))
                                        .putData("body", nextMsg.getPayload().getText())
                                        .putData("title", nextMsg.getPayload().getTitle())
                                        .putData("externalId", channelMessageId)
                                        .putData("destAdd", nextMsg.getTo().getUserID())
                                        .putData("fcmDestAdd", data.get("fcmToken"))
                                        .putData("click_action", click_action)
                                        .build();
                                uniqueUserSet.add(nextMsg.getTo().getUserID());
                                String adapterId = nextMsg.getAdapterId();

                                if (adapterId != null) {
                                    List<Message> tempMsgList = adapterIdMap.computeIfAbsent(adapterId, k -> new ArrayList<>());
                                    tempMsgList.add(message);
                                    adapterIdMap.put(adapterId, tempMsgList);
                                } else {
                                    log.error("FirebaseNotificationAdapter:processOutBoundMessageF::AdapterId not found : " + nextMsg);
                                }
                                xMessageListCass.add(nextMsg);
                            } else {
                                log.error("FirebaseNotificationAdapter:processOutBoundMessageF:: Fcm Token not found : " + data);
                            }
                        });
                        // TODO Dynamic this code
                        String adapterId = adapterIdMap.keySet().stream().collect(Collectors.toList()).get(0);
                        return botService.getAdapterCredentials(adapterId)
                                .flatMap(new Function<JsonNode, Mono<List<XMessage>>>() {
                                    @Override
                                    public Mono<List<XMessage>> apply(JsonNode credentials) {
                                        if (credentials != null && credentials.path("serviceKey") != null) {
                                            log.info("service key data: " + credentials.path("serviceKey"));
                                            String serviceKey = credentials.path("serviceKey").toString();
                                            try {
                                                FirebaseMessaging firebaseMessaging = configureFirebaseMessaging(serviceKey, adapterId);
                                                List<Message> messageList = adapterIdMap.get(adapterId);
                                                if (messageList != null && !messageList.isEmpty() && firebaseMessaging != null) {
                                                    return sendNotificationBatch(firebaseMessaging, messageList, xMessageListCass, uniqueUserSet, startTime)
                                                            .map(new Function<List<SendResponse>, List<XMessage>>() {
                                                                @Override
                                                                public List<XMessage> apply(List<SendResponse> sendResponseList) {
                                                                    for (int i = 0; i < sendResponseList.size(); i++) {
                                                                        SendResponse sendResponse = sendResponseList.get(i);
                                                                        if (sendResponse != null && sendResponse.isSuccessful()) {
                                                                            log.info("FirebaseNotificationService: Notification triggered success: Message Id : " + sendResponse.getMessageId() + " Phone No : " + xMessageListCass.get(i).getTo());
                                                                            String messageId = extractMessageId(sendResponse.getMessageId());
//                                                                            MessageId oldMessageId = xMessageListCass.get(i).getMessageId();
//                                                                            oldMessageId.setChannelMessageId(messageId);
                                                                            xMessageListCass.get(i).setTo(xMessageListCass.get(i).getTo());
                                                                            xMessageListCass.get(i).getMessageId().setChannelMessageId(messageId);
                                                                            xMessageListCass.get(i).setMessageState(XMessage.MessageState.SENT);
                                                                        } else {
                                                                            log.error("FirebaseNotificationService: Notification not sent : Exception : " + sendResponse.getException() + " : For this User : Phone No : " + xMessageListCass.get(i).getTo());
                                                                            xMessageListCass.get(i).setTo(xMessageListCass.get(i).getTo());
                                                                            xMessageListCass.get(i).setMessageState(XMessage.MessageState.NOT_SENT);
                                                                            xMessageListCass.get(i).getMessageId().setChannelMessageId(null);
                                                                        }
                                                                    }
                                                                    return xMessageListCass;
                                                                }
                                                            });
                                                } else {
                                                    log.error("FirebaseNotificationAdapter:processOutBoundMessageF:: MessageList empty :" + messageList + " or firebaseMessage object null : " + firebaseMessaging);
                                                }
                                            } catch (Exception e) {
                                                log.error("FirebaseNotificationAdapter:processOutBoundMessageF::Exception: " + e.getMessage());
                                            }
                                        } else {
                                            log.error("Service key not found: " + credentials);
                                        }
                                        return Mono.just(new ArrayList<>());
                                    }
                                });
                    }
                }).flatMapMany(new Function<Mono<List<XMessage>>, Flux<XMessage>>() {
                    @Override
                    public Flux<XMessage> apply(Mono<List<XMessage>> listMono) {
                        return listMono.flatMapMany(new Function<List<XMessage>, Flux<XMessage>>() {
                            @Override
                            public Flux<XMessage> apply(List<XMessage> xMessageList) {
                                return Flux.fromIterable(xMessageList);
                            }
                        });
                    }
                });
    }

    private <T> Mono<T> toMono(ApiFuture<T> future) {
        return Mono.create(sink -> {
            ApiFutures.addCallback(future, new ApiFutureCallback<T>() {
                public void onSuccess(T result) {
                    sink.success(result);
                }

                public void onFailure(Throwable t) {
                    sink.error(t);
                }
            }, MoreExecutors.directExecutor());
        });
    }

    private Mono<List<SendResponse>> sendNotificationBatch(FirebaseMessaging firebaseMessaging, List<Message> messageList, List<XMessage> xMessageList, Set<String> uniqueUserSet, long startTime) {
        log.info("All messages processed: messageList count: " + messageList.size());
        try {
            return Mono.fromCallable(() -> {
                        try {
                            return firebaseMessaging.sendAllAsync(messageList);
                        } catch (Exception e) {
                            log.error("FirebaseNotificationAdapter:sendNotificationBatch:: Exception: " + e.getMessage());
                            throw new RuntimeException(e);
                        }
                    })
                    .flatMap(this::toMono)
                    .map(batchResponse -> batchResponse.getResponses().stream()
                            .collect(Collectors.toList()))
                    .onErrorResume(e -> {
                        log.error("FirebaseNotificationAdapter:sendNotificationBatch:: Exception: ", e);
                        return Mono.just(new ArrayList<>());
                    });
        } catch (Exception ex) {
            log.error("An Error occurred: " + ex.getMessage());
            return Mono.just(new ArrayList<>());
        }
    }

    private static String extractMessageId(String input) {
        String pattern = "(?:.*\\/messages\\/)(.+)";
        Pattern regex = Pattern.compile(pattern);
        Matcher matcher = regex.matcher(input);
        if (matcher.matches()) {
            return matcher.group(1);
        }
        log.error("FirebaseNotificationAdapter:extractMessageId::Invalid extract MessageId : " + input);
        return null;
    }


//    public List<XMessage> updateMessageIds(List<XMessage> xMessageList, BatchResponse response) {
//        List<SendResponse> sendResponses = response.getResponses();
//
//        // Iterate over each SendResponse
//        for (int i = 0; i < sendResponses.size(); i++) {
//            SendResponse sendResponse = sendResponses.get(i);
//            if (sendResponse.isSuccessful()) {
//                log.info("FirebaseNotificationService: Notification triggered success: Message Id : " + sendResponse.getMessageId());
//                String messageId = extractMessageId(sendResponse.getMessageId());
//                SenderReceiverInfo to = xMessageList.get(i).getTo();
//                MessageId oldMessageId = xMessageList.get(i).getMessageId();
//                oldMessageId.setChannelMessageId(messageId);
//                xMessageList.get(i).setTo(to);
//                xMessageList.get(i).setMessageId(oldMessageId);
//                xMessageList.get(i).setMessageState(XMessage.MessageState.SENT);
//            } else {
//                log.error("FirebaseNotificationService: Notification not sent : Exception : " + sendResponse.getException());
//                SenderReceiverInfo to = xMessageList.get(i).getTo();
//                xMessageList.get(i).setTo(to);
//                xMessageList.get(i).setMessageState(XMessage.MessageState.NOT_SENT);
//            }
//        }
//        return xMessageList;
//    }


//    private void sendNotificationBatch(FirebaseMessaging firebaseMessaging, List<Message> messageList, List<XMessage> xMessageList, Set<String> uniqueUserSet, long startTime) {
//
//        log.info("All messages processed: messageList count: " + messageList.size());
//        try {
//            int batchSize = 500;
//            int totalMessages = messageList.size();
//            for (int i = 0; i < totalMessages; i += batchSize) {
//                List<Message> batchMessages = messageList.subList(i, Math.min(i + batchSize, totalMessages));
//                List<XMessage> batchXMessages = xMessageList.subList(i, Math.min(i + batchSize, totalMessages));
//                try {
//                    ApiFuture<BatchResponse> sendAllFuture = firebaseMessaging.sendAllAsync(batchMessages);
//
//                    ApiFutures.addCallback(sendAllFuture, new ApiFutureCallback<BatchResponse>() {
//                        @Override
//                        public void onSuccess(BatchResponse response) {
//                            // Handle the batch response
//                            response.getResponses().forEach(sendResponse -> {
//                                if (sendResponse.isSuccessful()) {
//                                    log.info("FirebaseNotificationService: Notification triggered success: Message Id : " + sendResponse.getMessageId());
//                                } else if (sendResponse.getException() != null) {
//                                    log.error("FirebaseNotificationService: Notification not sent : Exception : " + sendResponse.getException());
//                                } else {
//                                    log.info("FirebaseNotificationService: All SendResponse messageId : " + sendResponse.getMessageId() + " Exception : " + sendResponse.getException());
//                                }
//                            });
//                            logTimeTaken(startTime, 0, "Notification processed by sendNotificationBatch:: onSuccess: %d ms");
//                            log.info("Notification:: Success: " + response.getSuccessCount() + " Failed: " + response.getFailureCount() + " --> Total Unique users : " + uniqueUserSet.size());
//                        }
//
//                        @Override
//                        public void onFailure(Throwable throwable) {
//                            // Handle the error
//                            if (throwable instanceof ApiException) {
//                                ApiException apiException = (ApiException) throwable;
//                                log.error("Failed to send notification. Error code: " + apiException.getStatusCode() + ", Message: " + apiException.getMessage());
//                            } else {
//                                log.error("Failed to send notification. Error: " + throwable.getMessage());
//                            }
//                        }
//                    });
//
////                    BatchResponse response = firebaseMessaging.sendAll(batchMessages);
////                    response.getResponses().forEach(sendResponse -> {
////                        if (sendResponse.isSuccessful()) {
////                            log.info("FirebaseNotificationService: Notification triggered success: Message Id : " + sendResponse.getMessageId());
////                        } else {
////                            log.error("FirebaseNotificationService: Notification not sent : Exception : " + sendResponse.getException());
////                        }
////                    });
////                    log.info("Notification:: Success: " + response.getSuccessCount() + " Failed: " + response.getFailureCount() + " --> Total Unique users : " + uniqueUserSet.size());
//                } catch (Exception ex) {
//                    log.error("An Error occurred: " + ex.getMessage());
//                }
//            }
//        } catch (Exception ex) {
//            log.error("An Error occurred: " + ex.getMessage());
//        }
//    }

    public FirebaseMessaging configureFirebaseMessaging(String serviceKey, String appName) throws Exception {
        InputStream serviceAccountStream = new ByteArrayInputStream(serviceKey.getBytes(StandardCharsets.UTF_8));
        GoogleCredentials googleCredentials = GoogleCredentials
                .fromStream(serviceAccountStream);
        FirebaseOptions firebaseOptions = FirebaseOptions
                .builder()
                .setCredentials(googleCredentials)
                .build();
        FirebaseApp existingApp = FirebaseApp.getApps().stream()
                .filter(app -> app.getName().equals(appName))
                .findFirst()
                .orElseGet(() -> {
                    return FirebaseApp.initializeApp(firebaseOptions, appName);
                });
        return FirebaseMessaging.getInstance(existingApp);
    }

    private void logTimeTaken(long startTime, int checkpointID, String formatedMsg) {
        long endTime = System.nanoTime();
        long duration = (endTime - startTime) / 1000000;
        if (formatedMsg == null) {
            log.info(String.format("CP-%d: %d ms", checkpointID, duration));
        } else {
            log.info(String.format(formatedMsg, duration));
        }
    }
}
