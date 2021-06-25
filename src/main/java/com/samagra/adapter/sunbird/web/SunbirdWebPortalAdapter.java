package com.samagra.adapter.sunbird.web;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.samagra.adapter.netcore.whatsapp.outbound.Text;
import com.samagra.adapter.provider.factory.AbstractProvider;
import com.samagra.adapter.provider.factory.IProvider;
import com.samagra.adapter.sunbird.web.inbound.SunbirdWebMessage;
import com.samagra.utils.BotService;
import com.samagra.utils.PropertiesCache;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import messagerosa.core.model.MessageId;
import messagerosa.core.model.SenderReceiverInfo;
import messagerosa.core.model.XMessage;
import messagerosa.core.model.XMessagePayload;
import messagerosa.dao.XMessageDAO;
import messagerosa.dao.XMessageRepo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;
import reactor.core.publisher.Flux;

import javax.xml.bind.JAXBException;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.List;

@Slf4j
@Getter
@Setter
@Builder
public class SunbirdWebPortalAdapter extends AbstractProvider implements IProvider {

    @Autowired
    @Qualifier("rest")
    private RestTemplate restTemplate;

    private BotService botservice;

    public XMessageRepo xmsgRepo;

    @Override
    public Flux<Boolean> processOutBoundMessageF(XMessage nextMsg) throws Exception {
        return null;
    }

    @Override
    public XMessage convertMessageToXMsg(Object message) throws JAXBException, JsonProcessingException {
        SunbirdWebMessage webMessage = (SunbirdWebMessage) message;
        SenderReceiverInfo from = SenderReceiverInfo.builder().build();
        SenderReceiverInfo to = SenderReceiverInfo.builder().userID("admin").build();
        XMessage.MessageState messageState = XMessage.MessageState.REPLIED;
        MessageId messageIdentifier = MessageId.builder().build();

        XMessagePayload xmsgPayload = XMessagePayload.builder().build();
        long lastMsgId = 0;
        String appName = "";
        String adapter = "";
        log.info("Sunbird Adapter Test");
        xmsgPayload.setText(webMessage.getText());
        from.setUserID(webMessage.getFrom());
        appName = getAppName(from,"");
        adapter = botservice.getCurrentAdapter(appName);
        List<XMessageDAO> msg1 = xmsgRepo.findAllByUserIdOrderByTimestamp(from.getUserID());
        if (msg1.size() > 0) {
            XMessageDAO msg0 = msg1.get(0);
            lastMsgId = msg0.getId();
        }

        return XMessage.builder()
                .app(appName)
                .to(to)
                .from(from)
                .adapterId(adapter)
                .channelURI("web")
                .providerURI("sunbird")
                .messageState(messageState)
                .messageId(messageIdentifier)
                .timestamp(Timestamp.valueOf(LocalDateTime.now()).getTime())
                .payload(xmsgPayload)
                .lastMessageID(String.valueOf(lastMsgId)).build();
    }


    @Override
    public void processOutBoundMessage(XMessage nextMsg) throws Exception {
        log.info("next question to user is {}", nextMsg.toXML());

        //TODO: Need OUTBOUND URL for Message Push
        UriComponentsBuilder builder = UriComponentsBuilder.fromHttpUrl(PropertiesCache.getInstance().getProperty("DIKSHA_OUTBOUND"));
        Text t = Text.builder().content(nextMsg.getPayload().getText()).previewURL("false").build();
        Text[] texts = {t};

    }


    private String getAppName(SenderReceiverInfo from, String text) {
        String appName = null;
        try {
            appName = botservice.getCampaignFromStartingMessage(text);
            if(appName == null){
                try{
                    XMessageDAO xMessageLast = xmsgRepo.findTopByUserIdAndMessageStateOrderByTimestampDesc(from.getUserID(), "SENT");
                    appName = xMessageLast.getApp();
                }catch (Exception e2){
                    XMessageDAO xMessageLast = xmsgRepo.findTopByUserIdAndMessageStateOrderByTimestampDesc(from.getUserID(), "SENT");
                    appName = xMessageLast.getApp();
                }
            }
        } catch (Exception e) {}
        return appName;
    }
}
