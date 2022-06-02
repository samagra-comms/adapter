package com.uci.adapter.firebase.web;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.uci.adapter.provider.factory.AbstractProvider;
import com.uci.adapter.provider.factory.IProvider;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import messagerosa.core.model.MessageId;
import messagerosa.core.model.XMessage;
import reactor.core.publisher.Mono;

import javax.xml.bind.JAXBException;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.function.Function;

@Slf4j
@Getter
@Setter
@Builder
public class FirebaseMessageAdapter  extends AbstractProvider implements IProvider {
    @Override
    public Mono<XMessage> convertMessageToXMsg(Object message) throws JAXBException, JsonProcessingException {
        return null;
    }

    @Override
    public Mono<XMessage> processOutBoundMessageF(XMessage nextMsg) throws Exception {
        Map<String, String> meta = nextMsg.getTo().getMeta();
        if(meta != null && meta.get("fcmToken") != null) {
            return (new FirebaseMessagingService()).sendNotificationMessage(meta.get("fcmToken"), "Firebase Notification", nextMsg.getPayload().getText())
                    .map(new Function<Boolean, XMessage>() {
                        @Override
                        public XMessage apply(Boolean result) {
                            if (result) {
                                nextMsg.setMessageId(MessageId.builder().channelMessageId(LocalDateTime.now().toString()).build());
                                nextMsg.setMessageState(XMessage.MessageState.SENT);
                            }
                            return nextMsg;
                        }
                    });
        }
        return null;
    }
}
