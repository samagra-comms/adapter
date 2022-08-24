package com.uci.adapter.cdac;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.uci.adapter.provider.factory.AbstractProvider;
import com.uci.adapter.provider.factory.IProvider;
import com.uci.dao.repository.XMessageRepository;
import com.uci.utils.BotService;
import lombok.extern.slf4j.Slf4j;
import messagerosa.core.model.MessageId;
import messagerosa.core.model.XMessage;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import java.util.function.Function;

@Slf4j
@Qualifier("cdacSMSBulkAdapter")
@Service
public class CdacBulkSmsAdapter extends AbstractProvider implements IProvider {
    @Autowired
    public XMessageRepository xmsgRepo;

    @Autowired
    public BotService botService;

    @Autowired
    public CdacService cdacService;

    @Override
    public Mono<XMessage> convertMessageToXMsg(Object msg) throws JsonProcessingException {
        // Build xMessage => Most calls would be to update the status of Messages
        return Mono.just(XMessage.builder().build());
    }

    @Override
    public Mono<XMessage> processOutBoundMessageF(XMessage nextMsg) throws Exception {
        return botService.getAdapterCredentials(nextMsg.getAdapterId()).map(new Function<JsonNode, XMessage>() {
            @Override
            public XMessage apply(JsonNode credentials) {
                if (credentials != null && !credentials.isEmpty()
                    && credentials.get("username") != null && credentials.get("password") != null
                    && credentials.get("senderId") != null && credentials.get("secureKey") != null) {
                    String templateId = nextMsg.getTransformers().get(0).getMetaData().get("templateId");
                    String response = cdacService.sendUnicodeSMS(
                            credentials.get("username").asText(),
                            credentials.get("password").asText(),
                            nextMsg.getPayload().getText(),
                            credentials.get("senderId").asText(),
                            nextMsg.getTo().getUserID(),
                            credentials.get("secureKey").asText(),
                            templateId);
                    if (response != null) {
                        String splitResponse[] = response.split(",");
                        nextMsg.setMessageState(XMessage.MessageState.SENT);
                        if (splitResponse[1] != null && !splitResponse[1].isEmpty()) {
                            nextMsg.setMessageId(MessageId.builder().channelMessageId(splitResponse[1].replaceFirst("MsgID = ", "")).build());
                        }
                        return nextMsg;
                    } else {
                        log.error("No Response from cdac api");
                        nextMsg.setMessageState(XMessage.MessageState.NOT_SENT);
                        return nextMsg;
                    }
                } else {
                    log.error("Credentials not found");
                    nextMsg.setMessageState(XMessage.MessageState.NOT_SENT);
                    return nextMsg;
                }
            }
        });


    }

    public TrackDetails getLastTrackingReport(String campaignID) throws Exception {
//        Application campaign = BotService.getCampaignFromID(campaignID);
//        String appName = (String) campaign.data.get("appName");
////        XMessageDAO xMessage =
//              return   xmsgRepo.findFirstByAppOrderByTimestampDesc(appName).map(new Function<XMessageDAO, TrackDetails>() {
//                    @Override
//                    public TrackDetails apply(XMessageDAO xMessage) {
//                        return trackAndUpdate(xMessage);
//                    }
//                });
        return null;
    }
}