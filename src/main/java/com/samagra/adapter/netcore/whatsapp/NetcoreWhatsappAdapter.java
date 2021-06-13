package com.samagra.adapter.netcore.whatsapp;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.samagra.adapter.netcore.whatsapp.inbound.NetcoreWhatsAppMessage;
import com.samagra.adapter.netcore.whatsapp.outbound.OutboundMessage;
import com.samagra.adapter.netcore.whatsapp.outbound.SendMessageResponse;
import com.samagra.adapter.netcore.whatsapp.outbound.SingleMessage;
import com.samagra.adapter.netcore.whatsapp.outbound.Text;
import com.samagra.adapter.provider.factory.AbstractProvider;
import com.samagra.adapter.provider.factory.IProvider;
import com.samagra.user.BotService;
import io.fusionauth.domain.Application;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import messagerosa.core.model.MessageId;
import messagerosa.core.model.SenderReceiverInfo;
import messagerosa.core.model.XMessage;
import messagerosa.core.model.XMessagePayload;
import messagerosa.dao.XMessageDAO;
import messagerosa.dao.XMessageDAOUtills;
import messagerosa.dao.XMessageRepo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;
import reactor.core.publisher.Flux;

import java.net.URI;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

@Slf4j
@Getter
@Setter
@Builder
public class NetcoreWhatsappAdapter extends AbstractProvider implements IProvider {

    private final static String GUPSHUP_OUTBOUND = "https://media.smsgupshup.com/GatewayAPI/rest";
    @Autowired
    @Qualifier("rest")
    private RestTemplate restTemplate;

    private BotService botservice;

    public XMessageRepo xmsgRepo;

    @Override
    public XMessage convertMessageToXMsg(Object msg) throws JsonProcessingException {
        NetcoreWhatsAppMessage message = (NetcoreWhatsAppMessage) msg;
        SenderReceiverInfo from = SenderReceiverInfo.builder().build();
        SenderReceiverInfo to = SenderReceiverInfo.builder().userID("admin").build();

        XMessage.MessageState messageState = XMessage.MessageState.REPLIED;
        messageState = XMessage.MessageState.REPLIED;
        MessageId messageIdentifier = MessageId.builder().build();

        XMessagePayload xmsgPayload = XMessagePayload.builder().build();
        long lastMsgId = 0;
        String appName = "";
        String adapter = "";

        log.info("test");

        if (message.getEventType() != null) {
            xmsgPayload.setText("");
            messageIdentifier.setChannelMessageId(message.getMessageId());
            from.setUserID(message.getMobile().substring(2));
            appName = getAppName(from, "");
            adapter = botservice.getCurrentAdapter(appName);

            String eventType = message.getEventType().toString().toUpperCase();

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
        } else if (message.getType().equalsIgnoreCase("text")) {
            //Actual Message with payload (user response)
            messageState = XMessage.MessageState.REPLIED;
            from.setUserID(message.getMobile().substring(2));

            appName = getAppName(from, message.getText().getText());
            adapter = botservice.getCurrentAdapter(appName);

            messageIdentifier.setReplyId(message.getReplyId());
            xmsgPayload.setText(message.getText().getText());

            messageIdentifier.setChannelMessageId(message.getMessageId());

            List<XMessageDAO> msg1 = xmsgRepo.findAllByUserIdOrderByTimestamp(from.getUserID());
            if (msg1.size() > 0) {
                XMessageDAO msg0 = msg1.get(0);
                lastMsgId = msg0.getId();
            }
        }else if (message.getType().equals("button")){
            from.setUserID(message.getMobile().substring(2));
            // Get the last message sent to this user using the reply-messageID
            // Get the app from that message
            // Get the buttonLinkedApp
            // Add the starting text as the first message.

            XMessageDAO lastMessage = xmsgRepo.findTopByUserIdAndMessageStateOrderByTimestampDesc(from.getUserID(), "SENT");
            lastMsgId = lastMessage.getId();
            appName = lastMessage.getApp();
            Application application = botservice.getButtonLinkedApp(appName);
            appName = application.name;
            xmsgPayload.setText((String) application.data.get("startingMessage"));
        }else{
            System.out.println("No Match for parsing");
        }

        if(message.getLocation() !=null) xmsgPayload.setText(message.getLocation());
        return XMessage.builder()
                .app(appName)
                .to(to)
                .from(from)
                .adapterId(adapter)
                .channelURI("WhatsApp")
                .providerURI("Netcore")
                .messageState(messageState)
                .messageId(messageIdentifier)
                .timestamp(getTimestamp(message.getEventType(), message.getTimestamp()))
                .payload(xmsgPayload)
                .lastMessageID(String.valueOf(lastMsgId)).build();
    }

    Long getTimestamp(String eventType, String timestamp) {
        return timestamp == null ? Timestamp.valueOf(LocalDateTime.now()).getTime() : Long.parseLong(timestamp)*1000;
//        if (eventType != null)
//            return timestamp == null ? Timestamp.valueOf(LocalDateTime.now()).getTime() : Long.parseLong(timestamp)*1000;
//        else{
//            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
//            LocalDateTime date = LocalDateTime.parse(timestamp, formatter);
//            return Timestamp.valueOf(date).getTime();
//        }
    }

    /**
     * @param from: User form the whom the message was received.
     * @param text: User's text
     * @return appName
     */
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

    @Override
    public void processOutBoundMessage(XMessage nextMsg) throws Exception {
        log.info("nextXmsg {}", nextMsg.toXML());
        callOutBoundAPI(nextMsg);
    }

    @Override
    public Flux<Boolean> processOutBoundMessageF(XMessage nextMsg) throws Exception {
        return null;
    }

    public XMessage callOutBoundAPI(XMessage xMsg) throws Exception {
        log.info("next question to user is {}", xMsg.toXML());
        // String url = "http://federation-service:9999/admin/v1/adapter/getCredentials/" + xMsg.getAdapterId();
        // NWCredentials credentials = restTemplate.getForObject(url, NWCredentials.class);

        String channel = "";
        String phoneNo = "";
        String text = "";

        channel = xMsg.getChannelURI().toLowerCase();
        phoneNo = "91" + xMsg.getTo().getUserID();

        UriComponentsBuilder builder = UriComponentsBuilder.fromHttpUrl(GUPSHUP_OUTBOUND).
                queryParam("v", "1.1").
                queryParam("format", "json").
                queryParam("auth_scheme", "plain").
                queryParam("extra", "Samagra").
                queryParam("data_encoding", "text").
                queryParam("messageId", "123456789");
        if (xMsg.getMessageState().equals(XMessage.MessageState.OPTED_IN)) {

        }  else if (xMsg.getMessageType() != null && xMsg.getMessageType().equals(XMessage.MessageType.HSM)){
            // OPT in user
            text = xMsg.getPayload().getText();
        }else if (xMsg.getMessageType() != null && xMsg.getMessageType().equals(XMessage.MessageType.HSM_WITH_BUTTON)){
            // OPT in user
            text = xMsg.getPayload().getText();
        }else if (xMsg.getMessageState().equals(XMessage.MessageState.REPLIED)) {
            text = xMsg.getPayload().getText();
        }else{}

        // SendMessage
        Text t = Text.builder().content(text).previewURL("false").build();
        Text[] texts = {t};

        SingleMessage msg = SingleMessage
                .builder()
                .from("461089f9-1000-4211-b182-c7f0291f3d45")
                .to(phoneNo)
                .recipientType("individual")
                .messageType("text")
                .header("custom_data")
                .text(texts)
                .build();
        SingleMessage[] messages = {msg};

        String token = "eyJ0eXAiOiJKV1QiLCJhbGciOiJIUzUxMiJ9.eyJzdWIiOiJuZXRjb3Jlc2FsZXNleHAiLCJleHAiOjI0MjUxMDI1MjZ9.ljC4Tvgz031i6DsKr2ILgCJsc9C_hxdo2Kw8iZp9tsVcCaKbIOXaFoXmpU7Yo7ob4P6fBtNtdNBQv_NSMq_Q8w";
        NWCredentials nc = NWCredentials.builder().build();
        nc.setToken(token);
        NetcoreService ns = new NetcoreService(nc);

        OutboundMessage outboundMessage = OutboundMessage.builder().message(messages).build();
        SendMessageResponse response = ns.sendText(outboundMessage);

        xMsg.setMessageId(MessageId.builder().channelMessageId(response.getData().getIdentifier()).build());
        xMsg.setMessageState(XMessage.MessageState.SENT);

        XMessageDAO dao = XMessageDAOUtills.convertXMessageToDAO(xMsg);
        xmsgRepo.save(dao);
        return xMsg;
    }

}