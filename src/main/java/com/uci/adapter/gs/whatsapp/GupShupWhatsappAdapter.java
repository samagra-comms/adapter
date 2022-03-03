package com.uci.adapter.gs.whatsapp;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.uci.adapter.gs.whatsapp.outbound.MessageType;
import com.uci.adapter.gs.whatsapp.outbound.MethodType;
import com.uci.adapter.netcore.whatsapp.outbound.interactive.Action;
import com.uci.adapter.netcore.whatsapp.outbound.interactive.list.Section;
import com.uci.adapter.netcore.whatsapp.outbound.interactive.list.SectionRow;
import com.uci.adapter.netcore.whatsapp.outbound.interactive.quickreply.Button;
import com.uci.adapter.netcore.whatsapp.outbound.interactive.quickreply.ReplyButton;
import com.uci.adapter.provider.factory.AbstractProvider;
import com.uci.adapter.provider.factory.IProvider;
import com.uci.dao.models.XMessageDAO;
import com.uci.dao.repository.XMessageRepository;
import com.uci.dao.utils.XMessageDAOUtils;
import com.uci.utils.BotService;
import com.uci.utils.cdn.samagra.MinioClientService;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import messagerosa.core.model.ButtonChoice;
import messagerosa.core.model.MessageId;
import messagerosa.core.model.SenderReceiverInfo;
import messagerosa.core.model.StylingTag;
import messagerosa.core.model.XMessage;
import messagerosa.core.model.XMessagePayload;

import org.jetbrains.annotations.NotNull;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;
import reactor.core.publisher.Mono;

import java.net.URI;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.*;
import java.util.function.Function;

@Getter
@Setter
class GWCredentials {
    String passwordHSM;
    String usernameHSM;
    String password2Way;
    String username2Way;
}

@Slf4j
@Getter
@Setter
@Builder
public class GupShupWhatsappAdapter extends AbstractProvider implements IProvider {

    private String gsApiKey = "test";

    private final static String GUPSHUP_OUTBOUND = "https://media.smsgupshup.com/GatewayAPI/rest";
    @Autowired
    @Qualifier("rest")
    private RestTemplate restTemplate;

    @Autowired
    private BotService botservice;

    public XMessageRepository xmsgRepo;

    @Value("${campaign.url}")
    public String CAMPAIGN_URL;
    
    @Autowired
    private MinioClientService minioClientService;

    @Override
    public Mono<XMessage> convertMessageToXMsg(Object msg) throws JsonProcessingException {
        GSWhatsAppMessage message = (GSWhatsAppMessage) msg;
        SenderReceiverInfo from = SenderReceiverInfo.builder().build();
        SenderReceiverInfo to = SenderReceiverInfo.builder().userID("admin").build();

        final XMessage.MessageState[] messageState = new XMessage.MessageState[1];
        messageState[0] = XMessage.MessageState.REPLIED;
        MessageId messageIdentifier = MessageId.builder().build();
        XMessage.MessageType messageType= XMessage.MessageType.TEXT;
        XMessagePayload xmsgPayload = XMessagePayload.builder().build();

        if (message.getResponse() != null) {
            String reportResponse = message.getResponse();
            List<GSWhatsappReport> participantJsonList = new ObjectMapper().readValue(reportResponse, new TypeReference<List<GSWhatsappReport>>() {
            });
            for (GSWhatsappReport reportMsg : participantJsonList) {
                log.info("reportMsg {}", new ObjectMapper().writeValueAsString(reportMsg));
                String eventType = reportMsg.getEventType();
                xmsgPayload.setText("");
                messageIdentifier.setChannelMessageId(reportMsg.getExternalId());
                from.setUserID(reportMsg.getDestAddr().substring(2));
                messageState[0] = getMessageState(eventType);
            }
            return Mono.just(processedXMessage(message, xmsgPayload, to, from, messageState[0], messageIdentifier, messageType));
        } else if (message.getType().equals("text")) {
            //Actual Message with payload (user response)
            from.setUserID(message.getMobile().substring(2));
            messageIdentifier.setReplyId(message.getReplyId());
            
            if (message.getType().equals("OPT_IN")) {
                messageState[0] = XMessage.MessageState.OPTED_IN;
            } else if (message.getType().equals("OPT_OUT")) {
                xmsgPayload.setText("stop-wa");
                messageState[0] = XMessage.MessageState.OPTED_OUT;
            } else {
                messageState[0] = XMessage.MessageState.REPLIED;
                xmsgPayload.setText(message.getText());
                messageIdentifier.setChannelMessageId(message.getMessageId());
            }
            return Mono.just(processedXMessage(message, xmsgPayload, to, from, messageState[0], messageIdentifier, messageType));
        } else if (message.getType().equals("interactive")) {
        	//Actual Message with payload (user response)
            from.setUserID(message.getMobile().substring(2));
            messageIdentifier.setReplyId(message.getReplyId());
            
            messageState[0] = XMessage.MessageState.REPLIED;
            xmsgPayload.setText(getInboundInteractiveContentText(message));
            messageIdentifier.setChannelMessageId(message.getMessageId());
            
            return Mono.just(processedXMessage(message, xmsgPayload, to, from, messageState[0], messageIdentifier, messageType));
        } else if (message.getType().equals("location")) {
        	//Actual Message with payload (user response)
            from.setUserID(message.getMobile().substring(2));
            messageIdentifier.setReplyId(message.getReplyId());
            
            messageState[0] = XMessage.MessageState.REPLIED;
            xmsgPayload.setText(getInboundLocationContentText(message));
            messageIdentifier.setChannelMessageId(message.getMessageId());
            
            return Mono.just(processedXMessage(message, xmsgPayload, to, from, messageState[0], messageIdentifier, messageType));
        }else if (message.getType().equals("button")) {
            from.setUserID(message.getMobile().substring(2));
            return Mono.just(processedXMessage(message, xmsgPayload, to, from, messageState[0],messageIdentifier, messageType));
        }
        return null;

    }
    
    /**
     * Get Text from Interactive Context Reply
     * @param message
     * @return
     */
    private String getInboundInteractiveContentText(GSWhatsAppMessage message) {
    	String text  = "";
    	String interactiveContent = message.getInteractive();
    	if(interactiveContent != null && !interactiveContent.isEmpty()) {
    		ObjectMapper mapper = new ObjectMapper();
        	try {
        		JsonNode node = mapper.readTree(interactiveContent);
    			log.info("interactive content node: "+node);
    	    	
    			String type = node.path("type") != null ? node.path("type").asText() : "";
    	    	
    			if(type.equalsIgnoreCase("list_reply")) {
    				if(node.path("list_reply").path("title") != null) {
    					text = node.path("list_reply").path("title").asText();
    				}
    	    	} else if(type.equalsIgnoreCase("button_reply")) {
    	    		if(node.path("button_reply").path("title") != null) {
    	    			text = node.path("button_reply").path("title").asText();
    	    		}
    	    	}
    		} catch (JsonProcessingException e) {
    			log.error("Exception in getInboundInteractiveContentText: "+e.getMessage());
    		}
    	}
    	log.info("Inbound interactive text: "+text);
    	return text;
    }
    
    private String getInboundLocationContentText(GSWhatsAppMessage message) {
    	String text  = "";
    	String locationContent = message.getLocation();
    	if(locationContent != null && !locationContent.isEmpty()) {
    		ObjectMapper mapper = new ObjectMapper();
        	try {
        		JsonNode node = mapper.readTree(locationContent);
    			log.info("locationcontent node: "+node);
    	    	
    			String longitude = node.path("longitude") != null ? node.path("longitude").asText() : "";
    			String latitude = node.path("latitude") != null ? node.path("latitude").asText() : "";
    	    	
    			text = (latitude+" "+longitude).trim();
    		} catch (JsonProcessingException e) {
    			log.error("Exception in getInboundLocationContentText: "+e.getMessage());
    		}
    	}
    	log.info("Inbound location text: "+text);
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

    private XMessage processedXMessage(GSWhatsAppMessage message, XMessagePayload xmsgPayload, SenderReceiverInfo to,
                                       SenderReceiverInfo from, XMessage.MessageState messageState,
                                       MessageId messageIdentifier, XMessage.MessageType messageType) {
//        if (message.getLocation() != null) xmsgPayload.setText(message.getLocation());
        return XMessage.builder()
                .to(to)
                .from(from)
                .channelURI("WhatsApp")
                .providerURI("gupshup")
                .messageState(messageState)
                .messageId(messageIdentifier)
                .messageType(messageType)
                .timestamp(message.getTimestamp() == null ? Timestamp.valueOf(LocalDateTime.now()).getTime() : message.getTimestamp())
                .payload(xmsgPayload).build();
    }


    /**
     * Not in use
     */
//    @Override
//    public void processOutBoundMessage(XMessage nextMsg) throws Exception {
//        log.info("nextXmsg {}", nextMsg.toXML());
//        callOutBoundAPI(nextMsg);
//    }

    @Override
    public Mono<XMessage> processOutBoundMessageF(XMessage nextMsg) throws Exception {
    	log.info("processOutBoundMessageF nextXmsg {}", nextMsg.toXML());
        return callOutBoundAPI(nextMsg);
    }

    public Mono<XMessage> callOutBoundAPI(XMessage xMsg) throws Exception {
        log.info("next question to user is {}", xMsg.toXML());
        return botservice.getGupshupAdpaterCredentials(xMsg.getAdapterId()).map(new Function<Map<String, String>, XMessage>() {
            @Override
            public XMessage apply(Map<String, String> credentials) {

                String text = xMsg.getPayload().getText();
                
                UriComponentsBuilder builder = getURIBuilder();
                if (xMsg.getMessageState().equals(XMessage.MessageState.OPTED_IN)) {
                	text += renderMessageChoices(xMsg.getPayload().getButtonChoices());
                	
                	builder = setBuilderCredentialsAndMethod(builder, MethodType.OPTIN.toString(), credentials.get("username2Way"), credentials.get("password2Way"));
                	builder.queryParam("channel", xMsg.getChannelURI().toLowerCase()).
                    	queryParam("phone_number", "91" + xMsg.getTo().getUserID()); 
                } else if (xMsg.getMessageType() != null && xMsg.getMessageType().equals(XMessage.MessageType.HSM)) {
                    optInUser(xMsg, credentials.get("usernameHSM"), credentials.get("passwordHSM"), credentials.get("username2Way"), credentials.get("password2Way"));
                    
                    text += renderMessageChoices(xMsg.getPayload().getButtonChoices());
                    builder = setBuilderCredentialsAndMethod(builder, MethodType.SIMPLEMESSAGE.toString(), credentials.get("usernameHSM"), credentials.get("passwordHSM"));
                    builder.queryParam("send_to", "91" + xMsg.getTo().getUserID()).
	                    queryParam("msg", text).
	                    queryParam("isHSM", true).
	                    queryParam("msg_type", MessageType.HSM.toString());
                } else if (xMsg.getMessageType() != null && xMsg.getMessageType().equals(XMessage.MessageType.HSM_WITH_BUTTON)) {
                    optInUser(xMsg, credentials.get("usernameHSM"), credentials.get("passwordHSM"), credentials.get("username2Way"), credentials.get("password2Way"));
                    
                    text += renderMessageChoices(xMsg.getPayload().getButtonChoices());
                    builder = setBuilderCredentialsAndMethod(builder, "SendMessage", credentials.get("usernameHSM"), credentials.get("passwordHSM"));
                    builder.queryParam("send_to", "91" + xMsg.getTo().getUserID()).
	                    queryParam("msg", text).
	                    queryParam("isTemplate", "true").
	                    queryParam("msg_type", MessageType.HSM.toString());
                } else if (xMsg.getMessageState().equals(XMessage.MessageState.REPLIED)) {
                    Boolean plainText = true;
                    
                    MessageType msgType = MessageType.TEXT;
                    
                    StylingTag stylingTag = xMsg.getPayload().getStylingTag() != null
			    			? xMsg.getPayload().getStylingTag() : null;
                    
                    builder = setBuilderCredentialsAndMethod(builder, getMethodTypeByStylingTag(stylingTag).toString(), credentials.get("username2Way"), credentials.get("password2Way"));
                    builder.queryParam("send_to", "91" + xMsg.getTo().getUserID()).
	                    queryParam("msg_type", getMessageTypeByStylingTag(stylingTag).toString());
                    
                    if(stylingTag != null) {
                    	if(isStylingTagMediaType(stylingTag) && minioClientService != null) {
                    		if(stylingTag.equals(StylingTag.IMAGE) 
                            		&& xMsg.getPayload().getMediaCaption() != null
                            		&& !xMsg.getPayload().getMediaCaption().isEmpty()
                            ) {
                            	String signedUrl = minioClientService.getCdnSignedUrl(text.trim());
                            	if(!signedUrl.isEmpty()) {
                                	builder.queryParam("media_url", signedUrl);
                                    builder.queryParam("caption", xMsg.getPayload().getMediaCaption());
                                    builder.queryParam("isHSM", false);
                                    plainText = false;
                            	}
                            } else if(stylingTag.equals(StylingTag.AUDIO) || stylingTag.equals(StylingTag.VIDEO)) {
                            	String signedUrl = minioClientService.getCdnSignedUrl(text.trim());
                            	if(!signedUrl.isEmpty()) {
                                    builder.queryParam("media_url", signedUrl);
                                    builder.queryParam("isHSM", false);
                                    plainText = false;
                            	}
                            }
                    	} else if(stylingTag.equals(StylingTag.LIST)) {
                    		String content = getOutboundListActionContent(xMsg);
               			 	log.info("list content:  "+content);
                    		if(!content.isEmpty()) {
                    			builder.queryParam("interactive_type", "list");
                    			builder.queryParam("action", content);
                    			builder.queryParam("msg", text);
                    			plainText = false;
                    		}
                    	} else if(stylingTag.equals(StylingTag.QUICKREPLYBTN)) {
                    		String content = getOutboundQRBtnActionContent(xMsg);
               			 	log.info("QR btn content:  "+content);
                    		if(!content.isEmpty()) {
                    			builder.queryParam("interactive_type", "dr_button");
                    			builder.queryParam("action", content);
                    			builder.queryParam("msg", text);
                    			plainText = false;
                    		}	
                    	}
                    }
                    if(plainText) {
                    	text += renderMessageChoices(xMsg.getPayload().getButtonChoices());
                    	builder.queryParam("msg", text);
                    }
                } else {
                }
                
                log.info(text);
                URI expanded = URI.create(builder.toUriString());
                log.info(expanded.toString());
                
                RestTemplate restTemplate = new RestTemplate();
                GSWhatsappOutBoundResponse response = restTemplate.getForObject(expanded, GSWhatsappOutBoundResponse.class);
                try {
					log.info("response ================{}", new ObjectMapper().writeValueAsString(response));
				} catch (JsonProcessingException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
					log.error("Error in callOutBoundAPI for objectmapper: "+e.getMessage());
				}
                xMsg.setMessageId(MessageId.builder().channelMessageId(response.getResponse().getId()).build());
                xMsg.setMessageState(XMessage.MessageState.SENT);

                XMessageDAO dao = XMessageDAOUtils.convertXMessageToDAO(xMsg);
                xmsgRepo.insert(dao);
                return xMsg;
            }
        });
    }
    
    /**
     * Get Content for a List Action for outbound message
     * @param xMsg
     * @return
     */
    private String getOutboundListActionContent(XMessage xMsg) {
    	ArrayList rows = new ArrayList();
		xMsg.getPayload().getButtonChoices().forEach(choice -> {
			SectionRow row = SectionRow.builder()
								.id(choice.getKey())
								.title(choice.getText())
								.build();
			rows.add(row);
		});
		
		Action action = Action.builder()
				.button("Options")
    			.sections(new Section[]{Section.builder()
    					.title("Choose an option")
    					.rows(rows)
    					.build()
    			})
    			.build();
		
		ObjectMapper mapper = new ObjectMapper();
		try {
			 String content = mapper.writeValueAsString(action);
			 JsonNode node = mapper.readTree(content);
			 ObjectNode data = mapper.createObjectNode();
			 
			 data.put("button", node.path("button"));
			 data.put("sections", node.path("sections"));
			 return mapper.writeValueAsString(data);
		} catch (JsonProcessingException e) {
			// TODO Auto-generated catch block
			log.error("Exception in getInteractiveListContent: "+e.getMessage());
		}
		return "";
    }
    
    
    /**
     * Get Content for a Quick reply btn Action for outbound message
     * @param xMsg
     * @return
     */
    private String getOutboundQRBtnActionContent(XMessage xMsg) {
    	ArrayList buttons = new ArrayList();
		xMsg.getPayload().getButtonChoices().forEach(choice -> {
			Button button = Button.builder()
    	         	.type("reply")
    	         	.reply(ReplyButton.builder()
    	         				.id(choice.getKey())
    	         				.title(choice.getText())
    	         				.build())
    	         	.build();
			buttons.add(button);
		});
	        
	    Action action = Action.builder()
    	    			.buttons(buttons)
    	    			.build();
	    
	    ObjectMapper mapper = new ObjectMapper();
		try {
			 String content = mapper.writeValueAsString(action);
			 JsonNode node = mapper.readTree(content);
			 ObjectNode data = mapper.createObjectNode();
			 
			 data.put("buttons", node.path("buttons"));
			 return mapper.writeValueAsString(data);
		} catch (JsonProcessingException e) {
			// TODO Auto-generated catch block
			log.error("Exception in getInteractiveQRBtnContent: "+e.getMessage());
		}
		return "";
    }
    
    
    /**
     * Check if styling tag is image/audio/video type
     * @return
     */
    private Boolean isStylingTagMediaType(StylingTag stylingTag) {
    	if(stylingTag.equals(StylingTag.IMAGE) || stylingTag.equals(StylingTag.AUDIO) || stylingTag.equals(StylingTag.VIDEO)) {
    		return true;
    	}
    	return false;
    }
    
    /**
     * Check if styling tag is list/quick reply button
     * @return
     */
    private Boolean isStylingTagIntercativeType(StylingTag stylingTag) {
    	if(stylingTag.equals(StylingTag.LIST) || stylingTag.equals(StylingTag.QUICKREPLYBTN)) {
    		return true;
    	}
    	return false;
    }
    
    /**
     * Get Message Method Type by given styling tag
     * @param stylingTag
     * @return
     */
    private MethodType getMethodTypeByStylingTag(StylingTag stylingTag) {
    	MethodType methodType = MethodType.SIMPLEMESSAGE;
    	
    	if(stylingTag != null) {
    		if(isStylingTagMediaType(stylingTag)) {
    			methodType = MethodType.MEDIAMESSAGE;
    		} else if(isStylingTagIntercativeType(stylingTag)) {
    			methodType = MethodType.SIMPLEMESSAGE;
    		}
    	}
    	return methodType;
    }
    
    /**
     * Get Message Type by given styling tag
     * @param stylingTag
     * @return
     */
    private MessageType getMessageTypeByStylingTag(StylingTag stylingTag) {
    	MessageType messageType = MessageType.TEXT;
    	
    	if(stylingTag != null) {
    		if(stylingTag.equals(StylingTag.IMAGE)) {
    			messageType = MessageType.IMAGE;
    		} else if(stylingTag.equals(StylingTag.AUDIO) ) {
    			messageType = MessageType.AUDIO;
    		} else if(stylingTag.equals(StylingTag.VIDEO) ) {
    			messageType = MessageType.VIDEO;
    		} else if(isStylingTagIntercativeType(stylingTag)) {
    			messageType = MessageType.TEXT;
    		}
    	}
    	return messageType;
    }
    
    /**
     * Get Uri builder with default parameters
     * @return
     */
    private UriComponentsBuilder getURIBuilder() {
    	return UriComponentsBuilder.fromHttpUrl(GUPSHUP_OUTBOUND).
                queryParam("v", "1.1").
                queryParam("format", "json").
                queryParam("auth_scheme", "plain").
                queryParam("extra", "Samagra").
                queryParam("data_encoding", "text").
                queryParam("messageId", "123456789");
    }
    
    private UriComponentsBuilder setBuilderCredentialsAndMethod(UriComponentsBuilder builder, String method, String username, String password) {
    	return builder.queryParam("method", method).
                queryParam("userid", username).
                queryParam("password", password);
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

    private void optInUser(XMessage xMsg, String usernameHSM, String passwordHSM, String username2Way, String password2Way) {
        UriComponentsBuilder optInBuilder = UriComponentsBuilder.fromHttpUrl(GUPSHUP_OUTBOUND).
                queryParam("v", "1.1").
                queryParam("format", "json").
                queryParam("auth_scheme", "plain").
                queryParam("method", "OPT_IN").
                queryParam("userid", usernameHSM).
                queryParam("password", passwordHSM).
                queryParam("channel", "WHATSAPP").
                queryParam("phone_number", "91" + xMsg.getTo().getUserID()).
                queryParam("messageId", "123456789");

        URI expanded = URI.create(optInBuilder.toUriString());
        System.out.println(expanded.toString());
        RestTemplate restTemplate = new RestTemplate();
        String result = restTemplate.getForObject(expanded, String.class);
        System.out.println(result);
    }
}