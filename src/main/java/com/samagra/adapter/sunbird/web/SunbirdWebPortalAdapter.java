package com.samagra.adapter.sunbird.web;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.samagra.adapter.provider.factory.AbstractProvider;
import com.samagra.adapter.provider.factory.IProvider;
import com.samagra.adapter.sunbird.web.inbound.SunbirdWebMessage;
import com.samagra.adapter.sunbird.web.outbound.OutboundMessage;
import com.samagra.adapter.sunbird.web.outbound.SunbirdMessage;
import com.samagra.adapter.sunbird.web.outbound.SunbirdWebResponse;
import com.samagra.utils.PropertiesCache;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import messagerosa.core.model.MessageId;
import messagerosa.core.model.SenderReceiverInfo;
import messagerosa.core.model.XMessage;
import messagerosa.core.model.XMessagePayload;
import org.jetbrains.annotations.NotNull;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.web.client.RestTemplate;
import reactor.core.publisher.Mono;

import javax.xml.bind.JAXBException;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.function.Function;

@Slf4j
@Getter
@Setter
@Builder
public class SunbirdWebPortalAdapter extends AbstractProvider implements IProvider {

    @Autowired
    @Qualifier("rest")
    private RestTemplate restTemplate;


    @Override
    public Mono<XMessage> convertMessageToXMsg(Object message) throws JAXBException, JsonProcessingException {
        SunbirdWebMessage webMessage = (SunbirdWebMessage) message;
        SenderReceiverInfo from = SenderReceiverInfo.builder().build();
        SenderReceiverInfo to = SenderReceiverInfo.builder().userID("admin").build();
        XMessage.MessageState messageState = XMessage.MessageState.REPLIED;
        MessageId messageIdentifier = MessageId.builder().build();

        XMessagePayload xmsgPayload = XMessagePayload.builder().build();
        log.info("XMessage Payload getting created >>>");
        xmsgPayload.setText(webMessage.getText());
        //Todo: How to get Button choices from normal text
        from.setUserID(webMessage.getFrom());
        return Mono.just(XMessage.builder()
                .to(to)
                .from(from)
                .channelURI("web")
                .providerURI("sunbird")
                .messageState(messageState)
                .messageId(messageIdentifier)
                .timestamp(Timestamp.valueOf(LocalDateTime.now()).getTime())
                .payload(xmsgPayload).build());
    }

    @Override
    public Mono<XMessage> processOutBoundMessageF(XMessage xMsg) throws Exception {
        OutboundMessage outboundMessage = getOutboundMessage(xMsg);
        SunbirdCredentials sc = getCredentials();
        String url =PropertiesCache.getInstance().getProperty("SUNBIRD_OUTBOUND");
        return SunbirdWebService.getInstance(sc).
                sendOutboundMessage(url, outboundMessage)
                .map(new Function<SunbirdWebResponse, XMessage>() {
            @Override
            public XMessage apply(SunbirdWebResponse sunbirdWebResponse) {
                if(sunbirdWebResponse != null){
                    xMsg.setMessageId(MessageId.builder().channelMessageId(sunbirdWebResponse.getId()).build());
                    xMsg.setMessageState(XMessage.MessageState.SENT);
                }
                return xMsg;
            }
        });
    }


    @Override
    public void processOutBoundMessage(XMessage nextMsg) throws Exception {
        log.info("next question to user is {}", nextMsg.toXML());
        callOutBoundAPI(nextMsg);
    }

    public XMessage callOutBoundAPI(XMessage xMsg) throws Exception{
        OutboundMessage outboundMessage = getOutboundMessage(xMsg);
        SunbirdCredentials sc = getCredentials();
        //Get the Sunbird Outbound Url for message push
        String url =PropertiesCache.getInstance().getProperty("SUNBIRD_OUTBOUND");
        SunbirdWebService webService = new SunbirdWebService(sc);
        SunbirdWebResponse response = webService.sendText(url, outboundMessage);
        if(null != response){
            xMsg.setMessageId(MessageId.builder().channelMessageId(response.getId()).build());
        }
        xMsg.setMessageState(XMessage.MessageState.SENT);
        return xMsg;
    }

    @NotNull
    private SunbirdCredentials getCredentials() {
        String token = PropertiesCache.getInstance().getProperty("SUNBIRD_TOKEN");
        SunbirdCredentials sc = SunbirdCredentials.builder().build();
        sc.setToken(token);
        return sc;
    }

    private OutboundMessage getOutboundMessage(XMessage xMsg) {
        SunbirdMessage sunbirdMessage = SunbirdMessage.builder().title(xMsg.getPayload().getText()).choices(xMsg.getPayload().getButtonChoices()).build();
        SunbirdMessage[] messages = {sunbirdMessage};
        OutboundMessage outboundMessage = OutboundMessage.builder().message(messages).build();
        return outboundMessage;
    }
}
