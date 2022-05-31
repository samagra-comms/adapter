package com.uci.adapter.pwa;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.uci.adapter.pwa.web.inbound.PwaWebMessage;
import messagerosa.core.model.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import javax.xml.bind.JAXBException;

class PwaWebPortalAdapterTest {

    PwaWebPortalAdapter pwaWebPortalAdapter;

    @BeforeEach
    public void setup(){
        pwaWebPortalAdapter = new PwaWebPortalAdapter(null,null,null);
    }

    @Test
    void convertMessageToXMsg() throws JAXBException, JsonProcessingException {
        PwaWebMessage pwaWebMessage = new PwaWebMessage();
        pwaWebMessage.setMessageId("d098abd0-6f80-4013-b9d5-95ee6e002f0b");
        pwaWebMessage.setChannel("diksha");
        pwaWebMessage.setAppId("appId");
        pwaWebMessage.setText("Hi UCI");
        pwaWebMessage.setFrom("lMmNcAOrHq9SUfS3AAAB");
        pwaWebMessage.setTo("68a4a249-20fa-5841-8a56-11008a891bab");
        pwaWebMessage.setUserId("890b1309-373d-4f3c-9753-3fd83fe3e5e8");
        pwaWebMessage.setContext(null);
        pwaWebPortalAdapter.convertMessageToXMsg(pwaWebMessage);
    }

    @Test
    void processOutBoundMessageF() throws Exception {
        XMessage msg = XMessage.builder()
                        .adapterId("44a9df72-3d7a-4ece-94c5-98cf26307324")
                        .messageId(new MessageId(null,"a11937fc-1fa2-4df4-acc0-f7340a78e4f8", "qCrRvKYGq0qM3WY1AAAB"))
                        .app("UCI Demo")
                        .messageType(XMessage.MessageType.TEXT)
                        .to(new SenderReceiverInfo("qCrRvKYGq0qM3WY1AAAB", null, "UCI Demo", null, false, false, null,  DeviceType.PHONE_PWA, "2e997723-6e48-4d3f-b8c9-16d44743a342", null))
                        .from(new SenderReceiverInfo("admin", null, null, null, false, false, null, null, null, null))
                        .channelURI("web")
                        .providerURI("pwa")
                        .timestamp(null)
                        .messageState(XMessage.MessageState.REPLIED)
                        .lastMessageID("")
                        .payload(XMessagePayload.builder().text("Hi Rozgar bot !!!").build())
                        .build();

        pwaWebPortalAdapter.processOutBoundMessageF(msg);
    }

    @Test
    void processOutBoundMessage() throws Exception {
        XMessage msg = XMessage.builder()
                .adapterId("44a9df72-3d7a-4ece-94c5-98cf26307324")
                .messageId(new MessageId(null,"a11937fc-1fa2-4df4-acc0-f7340a78e4f8", "qCrRvKYGq0qM3WY1AAAB"))
                .app("UCI Demo")
                .messageType(XMessage.MessageType.TEXT)
                .to(new SenderReceiverInfo("qCrRvKYGq0qM3WY1AAAB", null, "UCI Demo", null, false, false, null,  DeviceType.PHONE_PWA, "2e997723-6e48-4d3f-b8c9-16d44743a342", null))
                .from(new SenderReceiverInfo("admin", null, null, null, false, false, null, null, null, null))
                .channelURI("web")
                .providerURI("pwa")
                .timestamp(null)
                .messageState(XMessage.MessageState.REPLIED)
                .lastMessageID("")
                .payload(XMessagePayload.builder().text("Hi Rozgar bot !!!").build())
                .build();
        pwaWebPortalAdapter.processOutBoundMessage(msg);
    }

}