package com.uci.adapter.netcore.whatsapp;

import com.uci.adapter.cdn.FileCdnProvider;
import com.uci.adapter.netcore.whatsapp.inbound.NetcoreLocation;
import com.uci.adapter.netcore.whatsapp.inbound.NetcoreWhatsAppMessage;
import com.uci.adapter.netcore.whatsapp.outbound.MessageType;
import com.uci.adapter.netcore.whatsapp.outbound.OutboundMessage;
import com.uci.adapter.netcore.whatsapp.outbound.SendMessageResponse;
import com.uci.adapter.netcore.whatsapp.outbound.SingleMessage;
import com.uci.adapter.netcore.whatsapp.outbound.SingleOptInOutMessage;
import com.uci.adapter.netcore.whatsapp.outbound.Text;
import com.uci.adapter.netcore.whatsapp.outbound.interactive.Action;
import com.uci.adapter.netcore.whatsapp.outbound.interactive.InteractiveContent;
import com.uci.adapter.netcore.whatsapp.outbound.interactive.list.Section;
import com.uci.adapter.netcore.whatsapp.outbound.interactive.list.SectionRow;
import com.uci.adapter.netcore.whatsapp.outbound.interactive.quickreply.Button;
import com.uci.adapter.netcore.whatsapp.outbound.interactive.quickreply.ReplyButton;
import com.uci.adapter.netcore.whatsapp.outbound.media.Attachment;
import com.uci.adapter.netcore.whatsapp.outbound.media.AttachmentType;
import com.uci.adapter.netcore.whatsapp.outbound.media.MediaContent;
import com.uci.adapter.netcore.whatsapp.outbound.OutboundOptInOutMessage;
import com.uci.adapter.provider.factory.AbstractProvider;
import com.uci.adapter.provider.factory.IProvider;
import com.uci.adapter.cdn.service.SunbirdCloudMediaService;
import com.uci.adapter.utils.CommonUtils;
import com.uci.adapter.utils.MediaSizeLimit;
import com.uci.utils.BotService;
import com.uci.utils.bot.util.FileUtil;

import io.fusionauth.domain.Application;
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

import java.io.IOException;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
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

	@Autowired
	private MediaSizeLimit mediaSizeLimit;

	@Autowired
	private FileCdnProvider fileCdnProvider;

	/**
     * Convert Inbound Netcore Message To XMessage
     */
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
            xmsgPayload.setText(getInboundInteractiveContentText(message));

            messageIdentifier.setChannelMessageId(message.getMessageId());

            return Mono.just(processedXMessage(message, xmsgPayload, to, from, finalMessageState, messageIdentifier,messageType));
        } else if (message.getType().equalsIgnoreCase("location") && message.getLocation() != null) {
            //Actual Message with payload (user response)
            messageState = XMessage.MessageState.REPLIED;
            from.setUserID(message.getMobile().substring(2));

            XMessage.MessageState finalMessageState = messageState;
            messageIdentifier.setReplyId(message.getReplyId());

            xmsgPayload.setLocation(getInboundLocationParams(message.getLocation()));
            xmsgPayload.setText("");

            messageIdentifier.setChannelMessageId(message.getMessageId());

            return Mono.just(processedXMessage(message, xmsgPayload, to, from, finalMessageState, messageIdentifier,messageType));
        } else if (isInboundMediaMessage(message.getType())) {
            //Actual Message with payload (user response)
            messageState = XMessage.MessageState.REPLIED;
            from.setUserID(message.getMobile().substring(2));

            XMessage.MessageState finalMessageState = messageState;
            messageIdentifier.setReplyId(message.getReplyId());
            
            xmsgPayload.setMedia(getInboundMediaMessage(message));
        	
            xmsgPayload.setText("");
            
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

        } else if (message.getType().equalsIgnoreCase("error")) {
			return null;
		} else {
            System.out.println("No Match for parsing");
            return Mono.just(processedXMessage(message, xmsgPayload, to, from, messageState, messageIdentifier,messageType));

        }

    }
    
    /**
     * Check if inbound message is media type
     * @param type
     * @return
     */
    private Boolean isInboundMediaMessage(String type) {
    	if(type.equals("IMAGE") || type.equals("VIDEO") || type.equals("AUDIO") 
    			|| type.equals("VOICE") || type.equals("DOCUMENT")) {
    		return true;
    	}
    	return false;
    }
    
    /**
     * Get Inbound Media name/url
     * @param message
     * @return
     */
    private MessageMedia getInboundMediaMessage(NetcoreWhatsAppMessage message) {
    	Map<String, Object> mediaInfo = getMediaInfo(message);
    	Map<String, Object> mediaData = uploadInboundMediaFile(message.getMessageId(), mediaInfo.get("id").toString(), mediaInfo.get("mime_type").toString());
    	MessageMedia media = new MessageMedia();
    	media.setText(mediaData.get("name").toString());
    	media.setUrl(mediaData.get("url").toString());
    	media.setCategory((MediaCategory) mediaInfo.get("category"));
		if(mediaData.get("error") != null) {
			media.setMessageMediaError((MessageMediaError) mediaData.get("error"));
		}
		if(mediaData.get("size") != null) {
			media.setSize((Double) mediaData.get("size"));
		}
		return media;
    }
    
    /**
     * Get Media Id & mime type
     * @param message
     * @return
     */
    private Map<String, Object> getMediaInfo(NetcoreWhatsAppMessage message) {
    	Map<String, Object> result = new HashMap();
    	
    	String id = "";
    	String mime_type = "";
    	Object category = null;
    	if(message.getImageType() != null) {
    		id = message.getImageType().getId();
    		mime_type = message.getImageType().getMimeType();
    	} else if(message.getAudioType() != null) {
    		id = message.getAudioType().getId();
    		mime_type = message.getAudioType().getMimeType();
    	} else if(message.getVideoType() != null) {
    		id = message.getVideoType().getId();
    		mime_type = message.getVideoType().getMimeType();
    	} else if(message.getVoiceType() != null) {
    		id = message.getVoiceType().getId();
    		mime_type = message.getVoiceType().getMimeType();
    	} else if(message.getDocumentType() != null) {
    		id = message.getDocumentType().getId();
    		mime_type = message.getDocumentType().getMimeType();
    	}
    	
    	if(FileUtil.isFileTypeImage(mime_type)) {
    		category = MediaCategory.IMAGE;
    	} else if(FileUtil.isFileTypeAudio(mime_type)) {
    		category = MediaCategory.AUDIO;
    	} else if(FileUtil.isFileTypeVideo(mime_type)) {
    		category = MediaCategory.VIDEO;
    	} else if(FileUtil.isFileTypeDocument(mime_type)) {
    		category = MediaCategory.FILE;
    	}
    	
    	log.info("File Id: "+id+", mime: "+mime_type+", category: "+category);
    	
    	result.put("id", id);
    	result.put("mime_type", mime_type);
    	result.put("category", category);
    	
    	return result;
    }
    
    /**
     * Upload media & get its url/name
     * @param messageId
     * @param id
     * @param mime_type
     * @return
     */
    private Map<String, Object> uploadInboundMediaFile(String messageId, String id, String mime_type) {
    	Map<String, Object> result = new HashMap();
    	
    	String name = "";
    	String url = "";

		if(!id.isEmpty() && !mime_type.isEmpty()) {
			try {
				log.info("Get netcore media by id:" + id);
				byte[] inputBytes = NewNetcoreService.getInstance().
						getMediaFile(id).readAllBytes();

				if (inputBytes != null) {
					// if file size is greater than MAX_SIZE_FOR_MEDIA than discard the file
					Double maxSizeForMedia = mediaSizeLimit.getMaxSizeForMedia(mime_type) ;
//					result.put("size", (double) responseBytes.length);
//
//					log.info("yash : maxSizeForMedia, " + maxSizeForMedia + " actualSizeOfMedia, " + responseBytes.length);
//					if (maxSizeForMedia != null && responseBytes.length > maxSizeForMedia) {
//						log.info("file size is("+ responseBytes.length +") greater than limit : " + maxSizeForMedia);
//						result.put("error", MessageMediaError.PAYLOAD_TO_LARGE);
//					} else{
//						String file = fileCdnProvider.uploadFileFromInputStream(new ByteArrayInputStream(responseBytes), mime_type, messageId);
//						name = file;
//						url = fileCdnProvider.getFileSignedUrl(file);
//						log.info("azure file name: " + name + ", url: " + url);
//					}

					String sizeError = FileUtil.validateFileSizeByInputBytes(inputBytes, maxSizeForMedia);
					if(sizeError.isEmpty()) {
						/* Unique File Name */
						name = FileUtil.getUploadedFileName(mime_type, messageId);
						String filePath = FileUtil.fileToLocalFromBytes(inputBytes, mime_type, name);
						if(!filePath.isEmpty()) {
							url = fileCdnProvider.uploadFileFromPath(filePath, name);
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
			} catch (IOException e) {
					e.printStackTrace();
			}
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
    private LocationParams getInboundLocationParams(NetcoreLocation message) {
        LocationParams location = new LocationParams();
        location.setLatitude(message.getLatitude());
        location.setLongitude(message.getLongitude());
        location.setAddress(message.getAddress());
        location.setUrl(message.getUrl());
        location.setName(message.getName());
    	
    	return location;
    }
    
    /**
     * Get Text from Interactive Context Reply
     * @param message
     * @return
     */
    private String getInboundInteractiveContentText(NetcoreWhatsAppMessage message) {
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
     * Process outbound messages - convert XMessage to Netcore Message Format and send to netcore api
     */
    @Override
    public Mono<XMessage> processOutBoundMessageF(XMessage xMsg) {
    	String phoneNo = "91" +xMsg.getTo().getUserID();
        SingleMessage message = getOutboundSingleMessage(xMsg, phoneNo);
        
        return NewNetcoreService.getInstance().
                sendOutboundMessage(OutboundMessage.builder().message(new SingleMessage[]{message}).build()).map(new Function<SendMessageResponse, XMessage>() {
            @Override
            public XMessage apply(SendMessageResponse sendMessageResponse) {
                if(sendMessageResponse != null){
                	if(sendMessageResponse.getStatus().equals("success")) {
                		xMsg.setMessageId(MessageId.builder().channelMessageId(sendMessageResponse.getData().getIdentifier()).build());
                        xMsg.setMessageState(XMessage.MessageState.SENT);
                	} else {
                		log.error("Netcore Outbound Api Error Response: "+sendMessageResponse.getError().getMessage());
                		return null;
                	}
                }
                return xMsg;
            }
        });


    }
    
    /**
     * Get Interactive Content object for Outbound Single Message object
     * @param xMsg
     * @param stylingTag
     * @return
     */
    private InteractiveContent getOutboundInteractiveContent(XMessage xMsg, StylingTag stylingTag) {
    	if(stylingTag != null && stylingTag.equals(StylingTag.LIST)) {
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
    		
    		String body = xMsg.getPayload().getText();
    		
    		return InteractiveContent.builder()
		        	.type("list")
		        	.action(new Action[]{action})
		        	.body(body)
		        	.build();
    	} else if(stylingTag != null && stylingTag.equals(StylingTag.QUICKREPLYBTN)) {
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
    	    
    	    String body = xMsg.getPayload().getText();
    	    
    	    return InteractiveContent.builder()
		        	.type("button")
		        	.action(new Action[]{action})
		        	.body(body)
		        	.build();
    	}
    	return null;
    }

	/**
	 * Get Media Content object for Outbound Single Message object
	 * @param xMsg
	 * @return
	 */
	private MediaContent getOutboundMessageMediaContent(XMessage xMsg) {
		MessageMedia media = xMsg.getPayload().getMedia();
		AttachmentType attachmentType = getAttachmentTypeByMediaCategory(media.getCategory());

		Attachment attachment = Attachment.builder()
				.attachment_url(media.getUrl())
				.attachment_type(attachmentType.toString())
				.caption(media.getText())
				.build();

		return MediaContent.builder()
				.attachments(new Attachment[] {attachment})
				.build();
	}
    
    /**
     * Get Outbound Single Message Object with text/interactive content/media content
     * @param xMsg
     * @param phoneNo
     * @return
     */
    private SingleMessage getOutboundSingleMessage(XMessage xMsg, String phoneNo) {
    	String source = System.getenv("NETCORE_WHATSAPP_SOURCE");
    	StylingTag stylingTag = xMsg.getPayload().getStylingTag() != null
								? xMsg.getPayload().getStylingTag() : null;
    	
    	if(stylingTag != null) {
			if(CommonUtils.isStylingTagIntercativeType(stylingTag) && validateInteractiveStylingTag(xMsg.getPayload())) {
    			//Menu List & Quick Reply Buttons
        		return SingleMessage
    			        .builder()
    			        .from(source)
    			        .to(phoneNo)
    			        .recipientType("individual")
    			        .messageType(MessageType.INTERACTIVE.toString())
    			        .header("custom_data")
    			        .interactiveContent(new InteractiveContent[]{
    			        	getOutboundInteractiveContent(xMsg, stylingTag)
    			        })
    			        .build();
    		}
    	} else if(xMsg.getPayload().getMedia() != null && xMsg.getPayload().getMedia().getUrl() != null) {
			//IMAGE/AUDIO/VIDEO/FILE
			return SingleMessage
					.builder()
					.from(source)
					.to(phoneNo)
					.recipientType("individual")
					.messageType(MessageType.MEDIA.toString())
					.header("custom_data")
					.mediaContent(new MediaContent[]{
							getOutboundMessageMediaContent(xMsg)
					})
					.build();
		}
    	//Plain List with text 
		String text = "";

	    if (xMsg.getMessageType() != null && xMsg.getMessageType().equals(XMessage.MessageType.HSM)) {
	    	// OPT in user
	    	optInUser(phoneNo);
	    	text = xMsg.getPayload().getText() + renderMessageChoices(xMsg.getPayload().getButtonChoices());
	    } else if (xMsg.getMessageType() != null && xMsg.getMessageType().equals(XMessage.MessageType.HSM_WITH_BUTTON)) {
	    	// OPT in user
	    	optInUser(phoneNo);
	    	text = xMsg.getPayload().getText()+ renderMessageChoices(xMsg.getPayload().getButtonChoices());
	    } else if (xMsg.getMessageState().equals(XMessage.MessageState.REPLIED)) {
	    	text = xMsg.getPayload().getText()+ renderMessageChoices(xMsg.getPayload().getButtonChoices());
	    }

	    // SendMessage
	    Text t = Text.builder().content(text).previewURL("false").build();
	    Text[] texts = {t};
	        
	    String content = t.getContent();
//	    log.info("before replace content: "+content);
	    content = content.replace("\\n", System.getProperty("line.separator"));
//	    log.info("after replace content: "+content);
	    t.setContent(content);
	    
	    return SingleMessage
		        .builder()
		        .from(source)
		        .to(phoneNo)
		        .recipientType("individual")
		        .messageType(MessageType.TEXT.toString())
		        .header("custom_data")
		        .text(texts)
		        .build();
    }
    
    /**
     * Opt in a user
     * @param phoneNo
     */
    private void optInUser(String phoneNo) {
    	SingleOptInOutMessage message = SingleOptInOutMessage
									        .builder()
									        .from("WEB")
									        .to(phoneNo)
									        .build();
    	
    	optInOutUser("optin", message);
    }
    
    /**
     * Opt out a user
     * @param phoneNo
     */
    private void optOutUser(String phoneNo) {
    	SingleOptInOutMessage message = SingleOptInOutMessage
									        .builder()
									        .from("WEB")
									        .to(phoneNo)
									        .build();
    	optInOutUser("optout", message);
    }
    
    /**
     * Opt in/out a user
     * @param type
     * @param message
     */
    private void optInOutUser(String type, SingleOptInOutMessage message) {
        NewNetcoreService.getInstance().
                sendOutboundOptInOutMessage(OutboundOptInOutMessage.builder().type(type).recipients(new SingleOptInOutMessage[]{message}).build()).map(new Function<SendMessageResponse, Boolean>() {
            @Override
            public Boolean apply(SendMessageResponse sendMessageResponse) {
                if(sendMessageResponse != null){
                	if(sendMessageResponse.getStatus().equals("success")) {
                		log.info("Netcore Outbound Api - Opt IN/OUT Success Response.");
                		return true;
                	} else {
                		log.error("Netcore Outbound Api - Opt IN/OUT Error Response: "+sendMessageResponse.getError().getMessage());
                	}
                }
                return false;
            }
        }).subscribe();
    }

	/**
	 * Get Attachment Type by given media category
	 * @param category
	 * @return
	 */
	private AttachmentType getAttachmentTypeByMediaCategory(MediaCategory category) {
		AttachmentType attachmentType = null;
		if(category.equals(MediaCategory.IMAGE)) {
			attachmentType = AttachmentType.IMAGE;
		} else if(category.equals(MediaCategory.AUDIO)) {
			attachmentType = AttachmentType.AUDIO;
		} else if(category.equals(MediaCategory.VIDEO)) {
			attachmentType = AttachmentType.VIDEO;
		} else if(category.equals(MediaCategory.FILE)) {
			attachmentType = AttachmentType.DOCUMENT;
		}
		return attachmentType;
	}

	/**
	 * validation for Interactive Styling Tag
	 * @Param XMessagePayload
	 * @return Boolean
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
}
