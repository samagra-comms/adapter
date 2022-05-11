package com.uci.adapter.cdac;

import messagerosa.core.model.MessageId;
import messagerosa.core.model.XMessage;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.util.function.Function;

@Service
public class CdacService {

    @Value("${cdac.base.url}")
    private String baseUrl;

    public Mono<XMessage> callOutBoundAPI(XMessage nextMsg) {
        String phoneNo = nextMsg.getTo().getUserID();
        String message = nextMsg.getPayload().getText();
        message = message.trim();
        StringBuilder finalmessage = new StringBuilder(message);
        String templateId = nextMsg.getTransformers().get(0).getMetaData().get("templateId");
        return WebClient.builder()
                .baseUrl(baseUrl)
                .build()
                .get()
                .uri(builder -> builder.path("/api/send_unicode_sms/")
                        .queryParam("message", finalmessage)
                        .queryParam("mobileNumber", phoneNo)
                        .queryParam("templateid", templateId).build())
                .retrieve()
                .bodyToMono(String.class)
                .map(new Function<String, XMessage>() {
                    @Override
                    public XMessage apply(String response) {
                        if (response != null && response.startsWith("402")) {
                            System.out.println("MESSAGE RESPONSE " + response);
                            String splitResponse[] = response.split(",");
                            nextMsg.setMessageState(XMessage.MessageState.SENT);
                            if (splitResponse[1] != null && !splitResponse[1].isEmpty()) {
                                nextMsg.setMessageId(MessageId.builder().channelMessageId(splitResponse[1].replaceFirst("MsgID = ", "")).build());
                            }
                            return nextMsg;
                        } else {
                            return null;
                        }
                    }
                });
    }
}
