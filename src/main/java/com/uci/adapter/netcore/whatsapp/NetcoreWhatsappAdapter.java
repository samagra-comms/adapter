package com.uci.adapter.netcore.whatsapp;

import com.uci.adapter.netcore.whatsapp.inbound.NetcoreWhatsAppMessage;
import com.uci.adapter.netcore.whatsapp.outbound.Action;
import com.uci.adapter.netcore.whatsapp.outbound.QuickReplyButton;
import com.uci.adapter.netcore.whatsapp.outbound.OutboundMessage;
import com.uci.adapter.netcore.whatsapp.outbound.ReplyButton;
import com.uci.adapter.netcore.whatsapp.outbound.ListSection;
import com.uci.adapter.netcore.whatsapp.outbound.ListSectionRow;
import com.uci.adapter.netcore.whatsapp.outbound.SendMessageResponse;
import com.uci.adapter.netcore.whatsapp.outbound.SingleMessage;
import com.uci.adapter.netcore.whatsapp.outbound.Text;
import com.uci.adapter.netcore.whatsapp.outbound.InterativeContent;
import com.uci.adapter.provider.factory.AbstractProvider;
import com.uci.adapter.provider.factory.IProvider;
import com.uci.utils.BotService;
import io.fusionauth.domain.Application;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import messagerosa.core.model.*;
import org.jetbrains.annotations.NotNull;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.client.RestTemplate;
import reactor.core.publisher.Mono;

import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.function.Function;

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

    @Override
    public Mono<XMessage> convertMessageToXMsg(Object msg) {
        NetcoreWhatsAppMessage message = (NetcoreWhatsAppMessage) msg;
        SenderReceiverInfo from = SenderReceiverInfo.builder().deviceType(DeviceType.PHONE).build();
        SenderReceiverInfo to = SenderReceiverInfo.builder().userID("admin").build();

        XMessage.MessageState messageState;
        messageState = XMessage.MessageState.REPLIED;
        MessageId messageIdentifier = MessageId.builder().build();
        XMessage.MessageType messageType= XMessage.MessageType.TEXT;
        XMessagePayload xmsgPayload = XMessagePayload.builder().build();
        String appName = "";
        
        if (message.getEventType() != null) {
            xmsgPayload.setText("");
            messageIdentifier.setChannelMessageId(message.getMessageId());
            from.setUserID(message.getMobile().substring(2));
            XMessage.MessageState messageState1;
            String eventType = message.getEventType().toUpperCase();
            messageState1 = getMessageState(eventType);
            return Mono.just(processedXMessage(message, xmsgPayload, to, from,  messageState1, messageIdentifier,messageType));

        } else if (message.getType().equalsIgnoreCase("text")) {
            //Actual Message with payload (user response)
            messageState = XMessage.MessageState.REPLIED;
            from.setUserID(message.getMobile().substring(2));

            XMessage.MessageState finalMessageState = messageState;
            messageIdentifier.setReplyId(message.getReplyId());
            xmsgPayload.setText(message.getText().getText());

            messageIdentifier.setChannelMessageId(message.getMessageId());

            return Mono.just(processedXMessage(message, xmsgPayload, to, from, finalMessageState, messageIdentifier,messageType));
        } else if (message.getType().equalsIgnoreCase("interactive")) {
            //Actual Message with payload (user response)
            messageState = XMessage.MessageState.REPLIED;
            from.setUserID(message.getMobile().substring(2));

            XMessage.MessageState finalMessageState = messageState;
            messageIdentifier.setReplyId(message.getReplyId());
            xmsgPayload.setText(getInteractiveContentText(message));

            messageIdentifier.setChannelMessageId(message.getMessageId());

            return Mono.just(processedXMessage(message, xmsgPayload, to, from, finalMessageState, messageIdentifier,messageType));
        } else if (message.getType().equals("button")) {
            from.setUserID(message.getMobile().substring(2));
            // Get the last message sent to this user using the reply-messageID
            // Get the app from that message
            // Get the buttonLinkedApp
            // Add the starting text as the first message.
            Application application = botservice.getButtonLinkedApp(appName);
            xmsgPayload.setText((String) application.data.get("startingMessage"));
            return Mono.just(processedXMessage(message, xmsgPayload, to, from, messageState, messageIdentifier,messageType));

        } else {
            System.out.println("No Match for parsing");
            return Mono.just(processedXMessage(message, xmsgPayload, to, from, messageState, messageIdentifier,messageType));

        }

    }
    
    private String getInteractiveContentText(NetcoreWhatsAppMessage message) {
    	String type = message.getInterativeContent().getType() != null
    						&& !message.getInterativeContent().getType().isEmpty() 
    						? message.getInterativeContent().getType() : "";
    	String text  = "";
    	if(type.equalsIgnoreCase("list_reply")) {
    		if(message.getInterativeContent().getListReply() != null 
    				|| message.getInterativeContent().getListReply().getTitle() != null) {
    			text = message.getInterativeContent().getListReply().getTitle();
    		}
    	} else if(type.equalsIgnoreCase("button_reply")) {
    		if(message.getInterativeContent().getButtonReply() != null 
    				|| message.getInterativeContent().getButtonReply().getTitle() != null) {
    			text = message.getInterativeContent().getButtonReply().getTitle();
    		}
    	}
    	log.info("interactive text: "+text);
    	return text;
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

    private XMessage processedXMessage(NetcoreWhatsAppMessage message, XMessagePayload xmsgPayload, SenderReceiverInfo to,
                                       SenderReceiverInfo from, XMessage.MessageState messageState,
                                       MessageId messageIdentifier, XMessage.MessageType messageType) {
        if (message.getLocation() != null) xmsgPayload.setText(message.getLocation());
        return XMessage.builder()
                .to(to)
                .from(from)
                .channelURI("WhatsApp")
                .providerURI("Netcore")
                .messageState(messageState)
                .messageId(messageIdentifier)
                .messageType(messageType)
                .timestamp(getTimestamp(message.getEventType(), message.getTimestamp()))
                .payload(xmsgPayload).build();
    }

    Long getTimestamp(String eventType, String timestamp) {
        return timestamp == null ? Timestamp.valueOf(LocalDateTime.now()).getTime() : Long.parseLong(timestamp) * 1000;
//        if (eventType != null)
//            return timestamp == null ? Timestamp.valueOf(LocalDateTime.now()).getTime() : Long.parseLong(timestamp)*1000;
//        else{
//            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
//            LocalDateTime date = LocalDateTime.parse(timestamp, formatter);
//            return Timestamp.valueOf(date).getTime();
//        }
    }

    /**
     * Not in use
     */
    @Override
    public void processOutBoundMessage(XMessage nextMsg) throws Exception {
        log.info("nextXmsg {}", nextMsg.toXML());
        callOutBoundAPI(nextMsg);
    }

    @Override
    public Mono<XMessage> processOutBoundMessageF(XMessage xMsg) {
    	String phoneNo = "91" +xMsg.getTo().getUserID();
        SingleMessage message = getOutboundSingleMessage(xMsg, phoneNo);
        
        return NewNetcoreService.getInstance(new NWCredentials(System.getenv("NETCORE_WHATSAPP_AUTH_TOKEN"))).
                sendOutboundMessage(OutboundMessage.builder().message(new SingleMessage[]{message}).build()).map(new Function<SendMessageResponse, XMessage>() {
            @Override
            public XMessage apply(SendMessageResponse sendMessageResponse) {
                if(sendMessageResponse != null){
                    xMsg.setMessageId(MessageId.builder().channelMessageId(sendMessageResponse.getData().getIdentifier()).build());
                    xMsg.setMessageState(XMessage.MessageState.SENT);
                }
                return xMsg;
            }
        });


    }
    
    private SingleMessage getOutboundSingleMessage(XMessage xMsg, String phoneNo) {
    	String messageType = "text";
    	String source = System.getenv("NETCORE_WHATSAPP_SOURCE");
//    	log.info("getStylingTag: "+xMsg.getPayload().getStylingTag());
    	String stylingTag = xMsg.getPayload().getStylingTag() != null 
    							&& !xMsg.getPayload().getStylingTag().isEmpty()
				    			? xMsg.getPayload().getStylingTag() : "plainList";
    	if(stylingTag.equals("list")) {
    		//Menu List
    		messageType = "interactive";
    		
    		ArrayList rows = new ArrayList();
    		xMsg.getPayload().getButtonChoices().forEach(choice -> {
    			ListSectionRow row = ListSectionRow.builder()
    								.id(choice.getKey())
//    								.title(choiceTextWithoutKey(choice.getText()))
    								.title(choice.getText())
    								.build();
    			rows.add(row);
    		});
    		
    		Action action = Action.builder()
    				.button("Options")
	    			.sections(new ListSection[]{ListSection.builder()
	    					.title("Choose an option")
	    					.rows(rows)
	    					.build()
	    			})
	    			.build();
    
    		String body = xMsg.getPayload().getText();
    
    		return SingleMessage
			        .builder()
			        .from(source)
			        .to(phoneNo)
			        .recipientType("individual")
			        .messageType("interactive")
			        .header("custom_data")
			        .interativeContent(new InterativeContent[]{
			        	InterativeContent.builder()
			        	.type("list")
			        	.action(new Action[]{action})
			        	.body(body)
			        	.build()
			        })
			        .build();
    	} else if(stylingTag.equals("buttonsForListItems")) {
    		//Quick to reply buttons
    		messageType = "interactive";
    		ArrayList buttons = new ArrayList();
    		xMsg.getPayload().getButtonChoices().forEach(choice -> {
    			QuickReplyButton button = QuickReplyButton.builder()
	    	         	.type("reply")
	    	         	.reply(ReplyButton.builder()
	    	         				.id(choice.getKey())
//	    	         				.title(choiceTextWithoutKey(choice.getText()))
	    	         				.title(choice.getText())
	    	         				.build())
	    	         	.build();
    			buttons.add(button);
    		});
    	        
    	    Action action = Action.builder()
	    	    			.buttons(buttons)
	    	    			.build();
    	    
    	    String body = xMsg.getPayload().getText();
    		
    		return SingleMessage
			        .builder()
			        .from(source)
			        .to(phoneNo)
			        .recipientType("individual")
			        .messageType("interactive")
			        .header("custom_data")
			        .interativeContent(new InterativeContent[]{
			        	InterativeContent.builder()
			        	.type("button")
			        	.action(new Action[]{action})
			        	.body(body)
			        	.build()
			        })
			        .build();
    	} else {
    		//Plain List with text 
    		String text = "";

    	    if (xMsg.getMessageType() != null && xMsg.getMessageType().equals(XMessage.MessageType.HSM)) {
    	    	// OPT in user
    	    	text = xMsg.getPayload().getText() + renderMessageChoices(xMsg.getPayload().getButtonChoices());
    	    } else if (xMsg.getMessageType() != null && xMsg.getMessageType().equals(XMessage.MessageType.HSM_WITH_BUTTON)) {
    	    	// OPT in user
    	    	text = xMsg.getPayload().getText()+ renderMessageChoices(xMsg.getPayload().getButtonChoices());
    	    } else if (xMsg.getMessageState().equals(XMessage.MessageState.REPLIED)) {
    	    	text = xMsg.getPayload().getText()+ renderMessageChoices(xMsg.getPayload().getButtonChoices());
    	    }

    	    // SendMessage
    	    Text t = Text.builder().content(text).previewURL("false").build();
    	    Text[] texts = {t};
    	        
    	    String content = t.getContent();
    	    log.info("before replace content: "+content);
    	    content = content.replace("\\n", System.getProperty("line.separator"));
    	    log.info("after replace content: "+content);
    	    t.setContent(content);
    	    
    	    return SingleMessage
			        .builder()
			        .from(source)
			        .to(phoneNo)
			        .recipientType("individual")
			        .messageType(messageType)
			        .header("custom_data")
			        .text(texts)
			        .build();
    	} 
    }
    
    /**
     * Get Choice Text - remove key(Numeric value Eg 1 or 1.) from text
     * @param choice_text
     * @return
     */
    private String choiceTextWithoutKey(String choice_text) {
    	String[] a = choice_text.split(" ");
		try {
			if(a[0] != null && !a[0].isEmpty()) {
				Integer.parseInt(a[0]);
				a = Arrays.copyOfRange(a, 1, a.length);
    			choice_text = String.join(" ", a);
    		}
		} catch (NumberFormatException ex) {
			String[] b = choice_text.split(".");
    		try {
    			if(b[0] != null && !b[0].isEmpty()) {
	    		    Integer.parseInt(b[0]);
	    		    b = Arrays.copyOfRange(b, 1, b.length);
	    			choice_text = String.join(" ", b);
    			}
    		} catch (NumberFormatException exc) {
    			// do nothing
    		} catch (ArrayIndexOutOfBoundsException exc) {
    		    // do nothing
    		}
		} catch (ArrayIndexOutOfBoundsException ex) {
			// do nothing
		}
    	return choice_text.trim();
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

    /**
     * Not in use
     */
    public XMessage callOutBoundAPI(XMessage xMsg) throws Exception {
        log.info("next question to user is {}", xMsg.toXML());
        // String url = "http://federation-service:9999/admin/v1/adapter/getCredentials/" + xMsg.getAdapterId();
        // NWCredentials credentials = restTemplate.getForObject(url, NWCredentials.class);

        String phoneNo = "";
        String text = "";

        phoneNo = "91" + xMsg.getTo().getUserID();

        if (xMsg.getMessageState().equals(XMessage.MessageState.OPTED_IN)) {

        } else if (xMsg.getMessageType() != null && xMsg.getMessageType().equals(XMessage.MessageType.HSM)) {
            // OPT in user
            text = xMsg.getPayload().getText()+ renderMessageChoices(xMsg.getPayload().getButtonChoices());;
        } else if (xMsg.getMessageType() != null && xMsg.getMessageType().equals(XMessage.MessageType.HSM_WITH_BUTTON)) {
            // OPT in user
            text = xMsg.getPayload().getText()+ renderMessageChoices(xMsg.getPayload().getButtonChoices());;
        } else if (xMsg.getMessageState().equals(XMessage.MessageState.REPLIED)) {
            text = xMsg.getPayload().getText()+ renderMessageChoices(xMsg.getPayload().getButtonChoices());;
        } else {
        }

        // SendMessage
        Text t = Text.builder().content(text).previewURL("false").build();
        Text[] texts = {t};

        SingleMessage msg = SingleMessage
                .builder()
                .from(System.getenv("NETCORE_WHATSAPP_SOURCE"))
                .to(phoneNo)
                .recipientType("individual")
                .messageType("text")
                .header("custom_data")
                .text(texts)
                .build();
        SingleMessage[] messages = {msg};

        NWCredentials nc = NWCredentials.builder().build();
        nc.setToken(System.getenv("NETCORE_WHATSAPP_AUTH_TOKEN"));
        NetcoreService ns = new NetcoreService(nc);

        OutboundMessage outboundMessage = OutboundMessage.builder().message(messages).build();
        SendMessageResponse response = ns.sendText(outboundMessage);

        xMsg.setMessageId(MessageId.builder().channelMessageId(response.getData().getIdentifier()).build());
        xMsg.setMessageState(XMessage.MessageState.SENT);


        return xMsg;
    }

}