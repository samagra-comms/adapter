package com.uci.adapter.cdac;

import com.fasterxml.jackson.core.JsonProcessingException;
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
        String response = cdacService.callOutBoundAPI(nextMsg).block();
        if (response != null) {
            System.out.println("MESSAGE RESPONSE " + response);
            String splitResponse[] = response.split(",");
            nextMsg.setMessageState(XMessage.MessageState.SENT);
            if (splitResponse[1] != null && !splitResponse[1].isEmpty()) {
                nextMsg.setMessageId(MessageId.builder().channelMessageId(splitResponse[1].replaceFirst("MsgID = ", "")).build());
            }
            return Mono.just(nextMsg);
        } else {
            return null;
        }
    }
}