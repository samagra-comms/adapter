package com.uci.adapter.netcore.whatsapp;

import com.azure.core.util.BinaryData;
import com.google.common.base.Splitter;
import com.google.common.collect.Iterables;
import com.uci.adapter.netcore.whatsapp.inbound.NetcoreLocation;
import com.uci.adapter.netcore.whatsapp.inbound.NetcoreWhatsAppMessage;
import com.uci.adapter.netcore.whatsapp.outbound.MessageType;
import com.uci.adapter.netcore.whatsapp.outbound.OutboundMessage;
import com.uci.adapter.netcore.whatsapp.outbound.SendMessageResponse;
import com.uci.adapter.netcore.whatsapp.outbound.SingleMessage;
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
import com.uci.adapter.provider.factory.AbstractProvider;
import com.uci.adapter.provider.factory.IProvider;
import com.uci.utils.BotService;
import com.uci.utils.azure.AzureBlobService;
import com.uci.utils.bot.util.FileUtil;

import io.fusionauth.domain.Application;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import messagerosa.core.model.*;

import org.apache.commons.lang3.tuple.Pair;
import org.jetbrains.annotations.NotNull;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.client.RestTemplate;
import reactor.core.publisher.Mono;

import java.io.File;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.function.Consumer;
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
    private AzureBlobService azureBlobService;

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

            xmsgPayload.setLocation(getPayloadLocationParams(message.getLocation()));
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
//    private String getInboundMediaContentText(NetcoreWhatsAppMessage message) {
//    	Map<String, Object> mediaInfo = getMediaInfo(message);
//    	Map<String, String> mediaData = uploadInboundMediaFile(message.getMessageId(), mediaInfo.get("id").toString(), mediaInfo.get("mime_type").toString());
//    	
//    	return mediaData.get("url").toString();
//    }
    
    /**
     * Get Inbound Media name/url
     * @param message
     * @return
     */
    private MessageMedia getInboundMediaMessage(NetcoreWhatsAppMessage message) {
    	Map<String, Object> mediaInfo = getMediaInfo(message);
    	Map<String, String> mediaData = uploadInboundMediaFile(message.getMessageId(), mediaInfo.get("id").toString(), mediaInfo.get("mime_type").toString());
    	MessageMedia media = new MessageMedia();
    	media.setText(mediaData.get("name").toString());
    	media.setUrl(mediaData.get("url").toString());
    	media.setCategory((MediaCategory) mediaInfo.get("category"));
    	
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
    private Map<String, String> uploadInboundMediaFile(String messageId, String id, String mime_type) {
    	Map<String, String> result = new HashMap();
    	
    	String name = "";
    	String url = "";
    	if(!id.isEmpty() && !mime_type.isEmpty()) {
    		log.info("Get netcore media by id:"+id);
    		InputStream response = NewNetcoreService.getInstance(new NWCredentials(System.getenv("NETCORE_WHATSAPP_AUTH_TOKEN"))).
                    getMediaFile(id);
    		if(response != null) {
        		String file = azureBlobService.uploadFileFromInputStream(response, mime_type, messageId);
        		name = file;
        		url = azureBlobService.getFileSignedUrl(file);
        		log.info("azure file name: "+name+", url: "+url);
    		} else {
        		log.info("response is empty");
        	}
    	}
    	result.put("name", name);
    	result.put("url", url);
    	
    	return result;
    }
    
    /**
     * Get text for Location 
     * @param message
     * @return
     */
//    private String getInboundLocationContentText(NetcoreWhatsAppMessage message) {
//    	String text = "";
//    	text = message.getLocation().getLatitude()+" "+message.getLocation().getLongitude();
//    	if(message.getLocation().getAddress() != null && !message.getLocation().getAddress().isEmpty()) {
//    		text += message.getLocation().getAddress();
//    	}
//    	if(message.getLocation().getName() != null && !message.getLocation().getName().isEmpty()) {
//    		text += message.getLocation().getName();
//    	}
//    	if(message.getLocation().getUrl() != null && !message.getLocation().getUrl().isEmpty()) {
//    		text += message.getLocation().getUrl();
//    	}
//    	return text.trim();
//    }
    
    /**
     * Get XMessage Payload Location params for inbound Location 
     * @param message
     * @return
     */
    private LocationParams getPayloadLocationParams(NetcoreLocation message) {
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
//        if (message.getLocation() != null) xmsgPayload.setText(message.getLocation());
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
//    @Override
//    public void processOutBoundMessage(XMessage nextMsg) throws Exception {
//        log.info("nextXmsg {}", nextMsg.toXML());
//        callOutBoundAPI(nextMsg);
//    }

    @Override
    public Mono<XMessage> processOutBoundMessageF(XMessage xMsg) {
    	String phoneNo = "91" +xMsg.getTo().getUserID();
        SingleMessage message = getOutboundSingleMessage(xMsg, phoneNo);
        
        return NewNetcoreService.getInstance(new NWCredentials(System.getenv("NETCORE_WHATSAPP_AUTH_TOKEN"))).
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
     * @param stylingTag
     * @return
     */
    private MediaContent getOutboundMediaContent(XMessage xMsg, StylingTag stylingTag) {
    	AttachmentType attachmentType = AttachmentType.IMAGE;
		if(stylingTag.equals(StylingTag.AUDIO)) {
			attachmentType = AttachmentType.AUDIO;
		} else if(stylingTag.equals(StylingTag.VIDEO)) {
			attachmentType = AttachmentType.VIDEO;	
		}
		
		String text = xMsg.getPayload().getText();
		text = text.replace("\n", "").replace("<br>", "").trim();
		
		String signedUrl = azureBlobService.getFileSignedUrl(text.trim());
    	
	    Attachment attachment = Attachment.builder()
    	    			.attachment_url(signedUrl)
    	    			.attachment_type(attachmentType.toString())
    	    			.build();
	    
	    if(stylingTag.equals(StylingTag.IMAGE) 
	    		&& xMsg.getPayload().getMediaCaption() != null 
	    		&& !xMsg.getPayload().getMediaCaption().isEmpty()) {
	    	attachment.setCaption(xMsg.getPayload().getMediaCaption());
	    }
	    
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
    		if(azureBlobService != null 
    				&& (
    					(stylingTag.equals(StylingTag.IMAGE) 
                    		&& xMsg.getPayload().getMediaCaption() != null
                    		&& !xMsg.getPayload().getMediaCaption().isEmpty()) 
    					|| (stylingTag.equals(StylingTag.AUDIO) || stylingTag.equals(StylingTag.VIDEO))
            		)
            ) {
    			//IMAGE/AUDIO/VIDEO
        		return SingleMessage
    			        .builder()
    			        .from(source)
    			        .to(phoneNo)
    			        .recipientType("individual")
    			        .messageType(MessageType.MEDIA.toString())
    			        .header("custom_data")
    			        .mediaContent(new MediaContent[]{
    			        	getOutboundMediaContent(xMsg, stylingTag)
    			        })
    			        .build();
    		} else if(isStylingTagInterativeType(stylingTag)) {
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
    	}
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
    private Boolean isStylingTagInterativeType(StylingTag stylingTag) {
    	if(stylingTag.equals(StylingTag.LIST) || stylingTag.equals(StylingTag.QUICKREPLYBTN)) {
    		return true;
    	}
    	return false;
    }
    /**
     * Get Choice Text - remove key(Numeric value Eg 1 or 1.) from text
     * @param choice_text
     * @return
     */
    /* Not in use - was in use for ReplyButton/SectionRow title choice text */
//    private String choiceTextWithoutKey(String choice_text) {
//    	String[] a = choice_text.split(" ");
//		try {
//			if(a[0] != null && !a[0].isEmpty()) {
//				Integer.parseInt(a[0]);
//				a = Arrays.copyOfRange(a, 1, a.length);
//    			choice_text = String.join(" ", a);
//    		}
//		} catch (NumberFormatException ex) {
//			String[] b = choice_text.split(".");
//    		try {
//    			if(b[0] != null && !b[0].isEmpty()) {
//	    		    Integer.parseInt(b[0]);
//	    		    b = Arrays.copyOfRange(b, 1, b.length);
//	    			choice_text = String.join(" ", b);
//    			}
//    		} catch (NumberFormatException exc) {
//    			// do nothing
//    		} catch (ArrayIndexOutOfBoundsException exc) {
//    		    // do nothing
//    		}
//		} catch (ArrayIndexOutOfBoundsException ex) {
//			// do nothing
//		}
//    	return choice_text.trim();
//    }

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
//    public XMessage callOutBoundAPI(XMessage xMsg) throws Exception {
//        log.info("next question to user is {}", xMsg.toXML());
//        // String url = "http://federation-service:9999/admin/v1/adapter/getCredentials/" + xMsg.getAdapterId();
//        // NWCredentials credentials = restTemplate.getForObject(url, NWCredentials.class);
//
//        String phoneNo = "";
//        String text = "";
//
//        phoneNo = "91" + xMsg.getTo().getUserID();
//
//        if (xMsg.getMessageState().equals(XMessage.MessageState.OPTED_IN)) {
//
//        } else if (xMsg.getMessageType() != null && xMsg.getMessageType().equals(XMessage.MessageType.HSM)) {
//            // OPT in user
//            text = xMsg.getPayload().getText()+ renderMessageChoices(xMsg.getPayload().getButtonChoices());;
//        } else if (xMsg.getMessageType() != null && xMsg.getMessageType().equals(XMessage.MessageType.HSM_WITH_BUTTON)) {
//            // OPT in user
//            text = xMsg.getPayload().getText()+ renderMessageChoices(xMsg.getPayload().getButtonChoices());;
//        } else if (xMsg.getMessageState().equals(XMessage.MessageState.REPLIED)) {
//            text = xMsg.getPayload().getText()+ renderMessageChoices(xMsg.getPayload().getButtonChoices());;
//        } else {
//        }
//
//        // SendMessage
//        Text t = Text.builder().content(text).previewURL("false").build();
//        Text[] texts = {t};
//
//        SingleMessage msg = SingleMessage
//                .builder()
//                .from(System.getenv("NETCORE_WHATSAPP_SOURCE"))
//                .to(phoneNo)
//                .recipientType("individual")
//                .messageType("text")
//                .header("custom_data")
//                .text(texts)
//                .build();
//        SingleMessage[] messages = {msg};
//
//        NWCredentials nc = NWCredentials.builder().build();
//        nc.setToken(System.getenv("NETCORE_WHATSAPP_AUTH_TOKEN"));
//        NetcoreService ns = new NetcoreService(nc);
//
//        OutboundMessage outboundMessage = OutboundMessage.builder().message(messages).build();
//        SendMessageResponse response = ns.sendText(outboundMessage);
//
//        xMsg.setMessageId(MessageId.builder().channelMessageId(response.getData().getIdentifier()).build());
//        xMsg.setMessageState(XMessage.MessageState.SENT);
//
//
//        return xMsg;
//    }

}