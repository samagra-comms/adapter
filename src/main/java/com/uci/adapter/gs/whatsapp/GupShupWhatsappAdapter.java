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
import com.uci.adapter.service.media.SunbirdCloudMediaService;
import com.uci.adapter.utils.CommonUtils;
import com.uci.adapter.utils.MediaSizeLimit;
import com.uci.dao.repository.XMessageRepository;
import com.uci.utils.BotService;
import com.uci.utils.bot.util.FileUtil;
import com.uci.utils.cdn.FileCdnProvider;

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
import org.springframework.web.util.UriComponentsBuilder;
import reactor.core.publisher.Mono;

import java.net.URI;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.ThreadLocalRandom;
import java.util.function.Consumer;
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
	private MediaSizeLimit mediaSizeLimit;

	@Autowired
	private FileCdnProvider fileCdnProvider;

	@Autowired
	private SunbirdCloudMediaService mediaService;

    /**
     * Convert Inbound Gupshup Message To XMessage
     */
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

		/**
		 * If for a replied message, message id is null, generate a new one, and set it
		 */
		if (message.getResponse() == null &&
				(message.getMessageId() == null || message.getMessageId().isEmpty())) {
			message.setMessageId(generateNewMessageId());
		}

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
            xmsgPayload.setLocation(getInboundLocationParams(message));
            xmsgPayload.setText("");
            messageIdentifier.setChannelMessageId(message.getMessageId());
            
            return Mono.just(processedXMessage(message, xmsgPayload, to, from, messageState[0], messageIdentifier, messageType));
        } else if (isInboundMediaMessage(message.getType())) {
        	//Actual Message with payload (user response)
            from.setUserID(message.getMobile().substring(2));
            messageIdentifier.setReplyId(message.getReplyId());
            
            messageState[0] = XMessage.MessageState.REPLIED;
            xmsgPayload.setText("");
            xmsgPayload.setMedia(getInboundMediaMessage(message));
            messageIdentifier.setChannelMessageId(message.getMessageId());
            
            return Mono.just(processedXMessage(message, xmsgPayload, to, from, messageState[0], messageIdentifier, messageType));
        } else if (message.getType().equals("button")) {
            from.setUserID(message.getMobile().substring(2));
            return Mono.just(processedXMessage(message, xmsgPayload, to, from, messageState[0],messageIdentifier, messageType));
        }
        return null;

    }
    
    /**
     * Check if inbound message is media type
     * @param type
     * @return
     */
    private Boolean isInboundMediaMessage(String type) {
    	if(type.equals("image") || type.equals("video") || type.equals("audio") 
    			|| type.equals("voice") || type.equals("document")) {
    		return true;
    	}
    	return false;
    }
    
    /**
     * Get Inbound Media name/url
     * @param message
     * @return
     */
    private MessageMedia getInboundMediaMessage(GSWhatsAppMessage message) {
    	Map<String, Object> mediaInfo = getMediaInfo(message);
    	Map<String, Object> mediaData = uploadInboundMediaFile(message.getMessageId(), mediaInfo.get("mediaUrl").toString(), mediaInfo.get("mime_type").toString());
    	MessageMedia media = new MessageMedia();
    	media.setText(mediaData.get("name").toString());
    	media.setUrl(mediaData.get("url").toString());
		media.setCategory((MediaCategory) mediaInfo.get("category"));
//		if(mediaData.get("url") != null && mediaData.get("url").toString().isEmpty()) {
//			media.setMessageMediaError(MessageMediaError.EMPTY_RESPONSE);
//		}
		media.setMessageMediaError((MessageMediaError) mediaData.get("error"));
		media.setSize((Double) mediaData.get("size"));
		//TODO: store media file size in media
    	return media;
    }
    
    /**
     * Get Media Id & mime type
     * @param message
     * @return
     */
    private Map<String, Object> getMediaInfo(GSWhatsAppMessage message) {
    	Map<String, Object> result = new HashMap();
    	
    	String mediaUrl = "";
    	String mime_type = "";
    	Object category = null;
    	String mediaContent = "";
    	if(message.getType().equals("image")) {
    		mediaContent = message.getImage();
    	} else if(message.getType().equals("audio")) {
    		mediaContent = message.getAudio();
    	} else if(message.getType().equals("voice")) {
    		mediaContent = message.getVoice();
    	} else if(message.getType().equals("video")) {
    		mediaContent = message.getVideo();
    	} else if(message.getType().equals("document")) {
    		mediaContent = message.getDocument();
    	}
    	
    	if(mediaContent != null && !mediaContent.isEmpty()) {
    		ObjectMapper mapper = new ObjectMapper();
        	try {
        		JsonNode node = mapper.readTree(mediaContent);
    			log.info("media content node: "+node);
    	    	
    			String url = node.path("url") != null ? node.path("url").asText() : "";
    			String signature = node.path("signature") != null ? node.path("signature").asText() : "";
    			mime_type = node.path("mime_type") != null ? node.path("mime_type").asText() : "";
    	
    			mediaUrl = url+signature;

				category = CommonUtils.getMediaCategoryByMimeType(mime_type);
    		} catch (JsonProcessingException e) {
    			log.error("Exception in getInboundInteractiveContentText: "+e.getMessage());
    		}
    	}
    	
    	result.put("mediaUrl", mediaUrl);
    	result.put("mime_type", mime_type);
    	result.put("category", category);
    	
    	return result;
    }
    
    /**
     * Upload media & get its url/name
     * @param messageId
     * @param mediaUrl
     * @param mime_type
     * @return
     */
    private Map<String, Object> uploadInboundMediaFile(String messageId, String mediaUrl, String mime_type) {
		Map<String, Object> result = new HashMap();

		Double maxSizeForMedia = mediaSizeLimit.getMaxSizeForMedia(mime_type);
    	String name = "";
    	String url = "";
    	if(!mediaUrl.isEmpty()) {
			byte[] inputBytes = FileUtil.getInputBytesFromUrl(mediaUrl);
			if(inputBytes != null) {
				String sizeError = FileUtil.validateFileSizeByInputBytes(inputBytes, maxSizeForMedia);
				if(sizeError.isEmpty()) {
					String filePath = FileUtil.fileToLocalFromBytes(inputBytes, mime_type, messageId);
					if(!filePath.isEmpty()) {
						url = mediaService.uploadFileFromPath(null, filePath);
					} else {
						result.put("size", 0d);
						result.put("error", MessageMediaError.EMPTY_RESPONSE);
					}
				} else {
					result.put("size", (double) inputBytes.length);
					result.put("error", MessageMediaError.PAYLOAD_TO_LARGE);
				}
			} else {
				result.put("size", 0d);
				result.put("error", MessageMediaError.EMPTY_RESPONSE);
			}
    	} else {
			result.put("size", 0d);
			result.put("error", MessageMediaError.EMPTY_RESPONSE);
		}
    	
    	result.put("name", name);
    	result.put("url", url);
    	
    	return result;
    }
    
    /**
     * Get XMessage Payload Location params for inbound Location 
     * @param message
     * @return
     */
    private LocationParams getInboundLocationParams(GSWhatsAppMessage message) {
    	Double longitude = null;
    	Double latitude = null;
    	String address = "";
    	String name = "";
    	String url = "";
    	String locationContent = message.getLocation();
    	if(locationContent != null && !locationContent.isEmpty()) {
    		ObjectMapper mapper = new ObjectMapper();
        	try {
        		JsonNode node = mapper.readTree(locationContent);
    			log.info("locationcontent node: "+node);
    	    	
    			longitude = node.path("longitude") != null ? Double.parseDouble(node.path("longitude").asText()) : null;
    			latitude = node.path("latitude") != null ? Double.parseDouble(node.path("latitude").asText()) : null;
    			address = node.path("address") != null ? node.path("address").asText() : "";
    			name = node.path("name") != null ? node.path("name").asText() : "";
    			url = node.path("url") != null ? node.path("url").asText() : "";
    	    	
    		} catch (JsonProcessingException e) {
    			log.error("Exception in getInboundLocationParams: "+e.getMessage());
    		}
    	}
    	
        LocationParams location = new LocationParams();
        location.setLatitude(latitude);
        location.setLongitude(longitude);
        location.setAddress(address);
        location.setUrl(url);
        location.setName(name);
    	
    	return location;
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
     * Process outbound messages - convert XMessage to Gupshup Message Format and send to gupshup api
     */
    @Override
    public Mono<XMessage> processOutBoundMessageF(XMessage xMsg) throws Exception {
		log.info("processOutBoundMessageF nextXmsg {}", xMsg.toXML());
		String adapterIdFromXML = xMsg.getAdapterId();
        String adapterId = "44a9df72-3d7a-4ece-94c5-98cf26307324";

		 return botservice.getAdapterCredentials(adapterIdFromXML).map(new Function<JsonNode, Mono<XMessage>>() {
				@Override
				public Mono<XMessage> apply(JsonNode credentials) {
					if(credentials != null && !credentials.isEmpty()) {
                        String text = xMsg.getPayload().getText();
    					UriComponentsBuilder builder = getURIBuilder();
    					if (xMsg.getMessageState().equals(XMessage.MessageState.OPTED_IN)) {
    						text += renderMessageChoices(xMsg.getPayload().getButtonChoices());

    						builder = setBuilderCredentialsAndMethod(builder, MethodType.OPTIN.toString(),  credentials.findValue("username2Way").asText(),  credentials.findValue("password2Way").asText());
    						builder.queryParam("channel", xMsg.getChannelURI().toLowerCase()).
    							queryParam("phone_number", "91" + xMsg.getTo().getUserID());
    					} else if (xMsg.getMessageType() != null && xMsg.getMessageType().equals(XMessage.MessageType.HSM)) {
    						optInUser(xMsg,  credentials.findValue("usernameHSM").asText(),  credentials.findValue("passwordHSM").asText(),  credentials.findValue("username2Way").asText(),  credentials.findValue("password2Way").asText());

    						text += renderMessageChoices(xMsg.getPayload().getButtonChoices());
    						builder = setBuilderCredentialsAndMethod(builder, MethodType.SIMPLEMESSAGE.toString(),  credentials.findValue("usernameHSM").asText(),  credentials.findValue("passwordHSM").asText());
    						builder.queryParam("send_to", "91" + xMsg.getTo().getUserID()).
    							queryParam("msg", text).
    							queryParam("isHSM", true).
    							queryParam("msg_type", MessageType.HSM.toString());
    					} else if (xMsg.getMessageType() != null && xMsg.getMessageType().equals(XMessage.MessageType.HSM_WITH_BUTTON)) {
    						optInUser(xMsg,  credentials.findValue("usernameHSM").asText(),  credentials.findValue("passwordHSM").asText(),  credentials.findValue("username2Way").asText(),  credentials.findValue("password2Way").asText());

    						text += renderMessageChoices(xMsg.getPayload().getButtonChoices());
    						builder = setBuilderCredentialsAndMethod(builder, "SendMessage",  credentials.findValue("usernameHSM").asText(),  credentials.findValue("passwordHSM").asText());
    						builder.queryParam("send_to", "91" + xMsg.getTo().getUserID()).
    							queryParam("msg", text).
    							queryParam("isTemplate", "true").
    							queryParam("msg_type", MessageType.HSM.toString());
    					} else if (xMsg.getMessageState().equals(XMessage.MessageState.REPLIED)) {
    						Boolean plainText = true;

    						MessageType msgType = MessageType.TEXT;

    						StylingTag stylingTag = xMsg.getPayload().getStylingTag() != null
    								? xMsg.getPayload().getStylingTag() : null;

    						builder = setBuilderCredentialsAndMethod(builder, MethodType.SIMPLEMESSAGE.toString(),  credentials.findValue("username2Way").asText(),  credentials.findValue("password2Way").asText());
    						builder.queryParam("send_to", "91" + xMsg.getTo().getUserID()).
    							queryParam("msg_type", MessageType.TEXT.toString());

    						/* For interactive type - list/button */
    						if(stylingTag != null && CommonUtils.isStylingTagIntercativeType(stylingTag)
    							&& validateInteractiveStylingTag(xMsg.getPayload())) {
    							if(stylingTag.equals(StylingTag.LIST)) {
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

    						/* For media */
    						if(xMsg.getPayload().getMedia() != null && xMsg.getPayload().getMedia().getUrl() != null) {
    							MessageMedia media = xMsg.getPayload().getMedia();
    							builder.replaceQueryParam("method", MethodType.MEDIAMESSAGE.toString())
    									.replaceQueryParam("msg_type", getMessageTypeByMediaCategory(media.getCategory()).toString());
    							builder.queryParam("media_url", media.getUrl());
    							builder.queryParam("caption", media.getText());
    							builder.queryParam("isHSM", false);
    							plainText = false;
    						}

    						/* For plain text */
    						if(plainText) {
    							text += renderMessageChoices(xMsg.getPayload().getButtonChoices());
    							builder.queryParam("msg", text);
    						}
    					}

    					log.info(text);
    					URI expanded = URI.create(builder.toUriString());
    					log.info(expanded.toString());

    					return GSWhatsappService.getInstance().sendOutboundMessage(expanded).map(new Function<GSWhatsappOutBoundResponse, XMessage>() {
    						@Override
    						public XMessage apply(GSWhatsappOutBoundResponse response) {
    							if(response != null && response.getResponse().getStatus().equals("success")){
    								xMsg.setMessageId(MessageId.builder().channelMessageId(response.getResponse().getId()).build());
    								xMsg.setMessageState(XMessage.MessageState.SENT);
    								return xMsg;
    							} else {
									log.error("Gupshup Whatsapp Message not sent: "+response.getResponse().getDetails());
									xMsg.setMessageState(XMessage.MessageState.NOT_SENT);
									return xMsg;
								}
    						}
    					}).doOnError(new Consumer<Throwable>() {
    						@Override
    						public void accept(Throwable throwable) {
    							log.error("Error in Send GS Whatsapp Outbound Message" + throwable.getMessage());
    						}
    					});
    				} else {
                        log.error("Credentials not found");
//                      xMsg.setMessageId(MessageId.builder().channelMessageId("").build());
                        xMsg.setMessageState(XMessage.MessageState.NOT_SENT);
                        return Mono.just(xMsg);
                    }
                }
			}).flatMap(new Function<Mono<XMessage>, Mono<? extends XMessage>>() {
				 @Override
				 public Mono<? extends XMessage> apply(Mono<XMessage> o) {
					 return o;
				 }
			 });
    }

	/**
	 * Validate if button choice option are valid for the list/button styling
	 * @param payload
	 * @return
	 */
	private boolean validateInteractiveStylingTag(XMessagePayload payload) {
//		String regx = "^[A-Za-z0-9 _(),+-.@#$%&*={}:;'<>]+$";
		if(payload.getStylingTag().equals(StylingTag.LIST)
				&& payload.getButtonChoices() != null
				&& payload.getButtonChoices().size() <= 10
		){
			for(ButtonChoice buttonChoice : payload.getButtonChoices()){
				if(buttonChoice.getText().length() > 24)
					return false;
			}
			return true;
		} else if(payload.getStylingTag().equals(StylingTag.QUICKREPLYBTN)
				&& payload.getButtonChoices() != null
				&& payload.getButtonChoices().size() <= 3
		){
			for(ButtonChoice buttonChoice : payload.getButtonChoices()){
				if(buttonChoice.getText().length() > 20 || buttonChoice.getKey().length() > 256)
					return false;
			}
			return true;
		} else{
			return false;
		}
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
	 * Get Message Type by given media category
	 * @param category
	 * @return
	 */
	private MessageType getMessageTypeByMediaCategory(MediaCategory category) {
		MessageType messageType = MessageType.TEXT;

		if(category != null) {
			if(category.equals(MediaCategory.IMAGE)) {
				messageType = MessageType.IMAGE;
			} else if(category.equals(MediaCategory.AUDIO)) {
				messageType = MessageType.AUDIO;
			} else if(category.equals(MediaCategory.VIDEO)) {
				messageType = MessageType.VIDEO;
			} else if(category.equals(MediaCategory.FILE)) {
				messageType = MessageType.DOCUMENT;
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
    
    /**
     * Convert Message Choices to Text
     * @param buttonChoices
     * @return
     */
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

	/**
	 * Generate new message id by gupshup format
	 * @return
	 */
	private String generateNewMessageId() {
		Long fMin = Long.parseLong("4000000000000000000"); //19 characters
		Long fMax = Long.parseLong("4999999999999999999"); //19 characters
		Long first = ThreadLocalRandom.current().nextLong(fMin, fMax);
		Long sMin = Long.parseLong("100000000000000000"); //18 characters
		Long sMax = Long.parseLong("999999999999999999"); //18 characters
		Long second = ThreadLocalRandom.current().nextLong(sMin, sMax);

		return first.toString()+"-"+second.toString();
	}
}
