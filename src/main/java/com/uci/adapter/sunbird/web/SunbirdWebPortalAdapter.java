package com.uci.adapter.sunbird.web;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.uci.adapter.provider.factory.AbstractProvider;
import com.uci.adapter.provider.factory.IProvider;
import com.uci.adapter.sunbird.web.inbound.SunbirdWebMessage;
import com.uci.adapter.sunbird.web.outbound.OutboundMessage;
import com.uci.adapter.sunbird.web.outbound.SunbirdMessage;
import com.uci.adapter.sunbird.web.outbound.SunbirdWebResponse;
import com.uci.adapter.utils.PropertiesCache;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import messagerosa.core.model.*;
import org.jetbrains.annotations.NotNull;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.web.client.RestTemplate;
import reactor.core.publisher.Mono;

import javax.xml.bind.JAXBException;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
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
        XMessage.MessageType messageType= XMessage.MessageType.TEXT;
        //Todo: How to get Button choices from normal text
        from.setUserID(webMessage.getFrom());
        
        /* To use later in outbound reply message's message id & to */
        messageIdentifier.setChannelMessageId(webMessage.getMessageId());
        messageIdentifier.setReplyId(webMessage.getTo());
        
        XMessage x = XMessage.builder()
                .to(to)
                .from(from)
                .channelURI("web")
                .providerURI("sunbird")
                .messageState(messageState)
                .messageId(messageIdentifier)
                .messageType(messageType)
                .timestamp(Timestamp.valueOf(LocalDateTime.now()).getTime())
                .payload(xmsgPayload).build();
        log.info("Current message :: " +  x.toString());
        return Mono.just(x);
    }

    @Override
    public Mono<XMessage> processOutBoundMessageF(XMessage xMsg) throws Exception {
        log.info("Sending message to transport socket :: " + xMsg.toXML());
        OutboundMessage outboundMessage = getOutboundMessage(xMsg);
        log.info("Sending final xmessage to transport socket :: " + xMsg.toXML());
        // String url = PropertiesCache.getInstance().getProperty("SUNBIRD_OUTBOUND");
        String url = "http://transport-socket.ngrok.samagra.io/botMsg/adapterOutbound";
        return SunbirdWebService.getInstance().
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
        //Get the Sunbird Outbound Url for message push
        // String url = PropertiesCache.getInstance().getProperty("SUNBIRD_OUTBOUND");
        String url = "http://transport-socket.ngrok.samagra.io/adapterOutbound";
        SunbirdWebService webService = new SunbirdWebService();
        SunbirdWebResponse response = webService.sendText(url, outboundMessage);
        if(null != response){
            xMsg.setMessageId(MessageId.builder().channelMessageId(response.getId()).build());
        }
        xMsg.setMessageState(XMessage.MessageState.SENT);
        return xMsg;
    }

//    @NotNull
//    private SunbirdCredentials getCredentials() {
//        String token = PropertiesCache.getInstance().getProperty("SUNBIRD_TOKEN");
//        SunbirdCredentials sc = SunbirdCredentials.builder().build();
//        sc.setToken(token);
//        return sc;
//    }

    private OutboundMessage getOutboundMessage(XMessage xMsg) throws JAXBException {
        SunbirdMessage sunbirdMessage = SunbirdMessage.builder()
        									.title(getTextMessage(xMsg))
        									.choices(this.getButtonChoices(xMsg))
        									.build();
        return OutboundMessage.builder()
        		.message(sunbirdMessage)
				.to(xMsg.getMessageId().getReplyId())
				.messageId(xMsg.getMessageId().getChannelMessageId())
				.build();
    }
    
    /**
     * Get Simplified Text Message
     * @param xMsg
     * @return String
     */
    private String getTextMessage(XMessage xMsg) {
    	XMessagePayload payload = xMsg.getPayload();
    	String text = payload.getText().replace("__", "");
    	text = text.replace("\n\n", "");
    	payload.setText(text);
    	return text;
    }
    
    /**
     * Get Button Choices with calculated keys
     * @param xMsg
     * @return ArrayList of ButtonChoices
     */
    private ArrayList<ButtonChoice> getButtonChoices(XMessage xMsg) {
    	ArrayList<ButtonChoice> choices = xMsg.getPayload().getButtonChoices();
    	if(choices != null) {
    		choices.forEach(c -> {
        		String[] a = c.getText().split(" ");
        		if(a[0] != null && !a[0].isEmpty()) {
        			String key = a[0].toString();
        			
        			c.setKey(key);
        			c.setText(c.getText().replaceFirst(key, "").trim());
        		}
        	});
    	}
    	return choices;
    }

    private String renderMessageChoices(ArrayList<ButtonChoice> buttonChoices) {
        StringBuilder processedChoicesBuilder = new StringBuilder("");
        if(buttonChoices != null){
            for(ButtonChoice choice:buttonChoices){
                processedChoicesBuilder.append(choice.getText()).append("\n");
            }
            String processedChoices = processedChoicesBuilder.toString();
            return processedChoices.substring(0,processedChoices.length()-1);
        }
        return "";
    }
}
