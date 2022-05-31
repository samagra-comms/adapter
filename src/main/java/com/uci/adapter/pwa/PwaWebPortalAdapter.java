package com.uci.adapter.pwa;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectWriter;
import com.uci.adapter.gs.whatsapp.GSWhatsAppMessage;
import com.uci.adapter.gs.whatsapp.outbound.MessageType;
import com.uci.adapter.provider.factory.AbstractProvider;
import com.uci.adapter.provider.factory.IProvider;
import com.uci.adapter.pwa.web.inbound.PwaWebMedia;
import com.uci.adapter.pwa.web.outbound.PwaWebResponse;
import com.uci.adapter.pwa.web.inbound.PwaWebMessage;
import com.uci.adapter.pwa.web.outbound.OutboundMessage;
import com.uci.adapter.pwa.web.outbound.PwaMessage;
import com.uci.adapter.pwa.web.outbound.PwaWebResponse;
import com.uci.adapter.utils.CommonUtils;
import com.uci.adapter.utils.PropertiesCache;
import com.uci.utils.bot.util.FileUtil;
import com.uci.utils.cdn.FileCdnProvider;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import messagerosa.core.model.*;
import org.apache.http.HttpStatus;
import org.jetbrains.annotations.NotNull;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.util.MimeTypeUtils;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;
import reactor.core.publisher.Mono;

import javax.xml.bind.JAXBException;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.*;
import java.util.function.Function;

@Slf4j
@Getter
@Setter
@Builder
public class PwaWebPortalAdapter extends AbstractProvider implements IProvider {

    @Autowired
    @Qualifier("rest")
    private RestTemplate restTemplate;

    private String assesOneLevelUpChar;
    private String assesGoToStartChar;

    @Autowired
    private FileCdnProvider fileCdnProvider;

    @Autowired
    private CommonUtils commonUtils;


    @Override
    public Mono<XMessage> convertMessageToXMsg(Object message) throws JAXBException, JsonProcessingException {
        PwaWebMessage webMessage = (PwaWebMessage) message;
        SenderReceiverInfo from = SenderReceiverInfo.builder().deviceType(DeviceType.PHONE_PWA).build();
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
        messageIdentifier.setReplyId(webMessage.getFrom());


        if(webMessage.getMedia() != null && webMessage.getMedia().getUrl() != null && webMessage.getMedia().getMimeType() != null) {
            String mimeType = webMessage.getMedia().getMimeType();
            if(isInboundMediaMessage(mimeType)){
                xmsgPayload.setMedia(getInboundMediaMessage(webMessage.getMedia()));
            }
        }

        XMessage x = XMessage.builder()
                .to(to)
                .from(from)
                .channelURI("web")
                .providerURI("pwa")
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
        String url = System.getenv("PWA_TRANSPORT_SOCKET_BASE_URL")+"/botMsg/adapterOutbound";
        ObjectWriter ow = new ObjectMapper().writer().withDefaultPrettyPrinter();
        try {
            String json = ow.writeValueAsString(outboundMessage);
            System.out.println("json:"+json);
        } catch (JsonProcessingException e) {
            System.out.println("json not converted:"+e.getMessage());
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
        return PwaWebService.getInstance().
                sendOutboundMessage(url, outboundMessage)
                .map(new Function<PwaWebResponse, XMessage>() {
            @Override
            public XMessage apply(PwaWebResponse pwaWebResponse) {
                if(pwaWebResponse != null){
                    xMsg.setMessageId(MessageId.builder().channelMessageId(pwaWebResponse.getId()).build());
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
        String url = System.getenv("PWA_TRANSPORT_SOCKET_BASE_URL")+"/adapterOutbound";
        PwaWebService webService = new PwaWebService();
        PwaWebResponse response = webService.sendText(url, outboundMessage);
        if(null != response){
            xMsg.setMessageId(MessageId.builder().channelMessageId(response.getId()).build());
        }
        xMsg.setMessageState(XMessage.MessageState.SENT);
        return xMsg;
    }

    private OutboundMessage getOutboundMessage(XMessage xMsg) throws JAXBException {
        StylingTag stylingTag = xMsg.getPayload().getStylingTag() != null
                ? xMsg.getPayload().getStylingTag() : null;
        PwaMessage pwaMessage = null;
        if(stylingTag != null) {
            if(isStylingTagMediaType(stylingTag)) {
                String text = xMsg.getPayload().getText();
                if (stylingTag.equals(StylingTag.IMAGE) || stylingTag.equals(StylingTag.AUDIO)
                        || stylingTag.equals(StylingTag.VIDEO) || stylingTag.equals(StylingTag.DOCUMENT)) {
                    String signedUrl = fileCdnProvider.getFileSignedUrl(text.trim());
                    if(!signedUrl.isEmpty()) {
                        pwaMessage = PwaMessage.builder()
                                .msg_type(stylingTag.toString().toUpperCase())
                                .caption(xMsg.getPayload().getMediaCaption())
                                .media_url(signedUrl)
                                .build();
                    }
                } else if(stylingTag.equals(StylingTag.IMAGE_URL) || stylingTag.equals(StylingTag.DOCUMENT_URL) || stylingTag.equals(StylingTag.AUDIO_URL)
                || stylingTag.equals(StylingTag.VIDEO_URL)){
                    String url = xMsg.getPayload().getText();
                    Integer respCode = commonUtils.isUrlExists(url);
                    if(respCode != null && respCode == HttpStatus.SC_OK){
                        pwaMessage = PwaMessage.builder()
                                .msg_type(commonUtils.convertMessageType(stylingTag.toString().toLowerCase()))
                                .caption(xMsg.getPayload().getMediaCaption())
                                .media_url(url)
                                .build();
                    } else {
                        pwaMessage = PwaMessage.builder()
                                .title(url)
                                .msg_type(StylingTag.TEXT.toString().toUpperCase())
                                .build();
                    }
                }
            } else{
                pwaMessage = PwaMessage.builder()
                        .title(getTextMessage(xMsg))
                        .choices(this.getButtonChoices(xMsg))
                        .msg_type(StylingTag.TEXT.toString().toUpperCase())
                        .caption(xMsg.getPayload().getMediaCaption())
                        .build();
            }
        } else {
            pwaMessage = PwaMessage.builder()
                    .title(getTextMessage(xMsg))
                    .choices(this.getButtonChoices(xMsg))
                    .msg_type(StylingTag.TEXT.toString().toUpperCase())
                    .caption(xMsg.getPayload().getMediaCaption())
                    .build();
        }
        return OutboundMessage.builder()
        		.message(pwaMessage)
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
//    	text = text.replaceAll("\n", "<br>");
    	text = text.replaceAll("\\\\n", "\n");
    	payload.setText(text);
    	return text;
    }

    /**
     * Get Button Choices with calculated keys
     * @param xMsg
     * @return ArrayList of ButtonChoices
     */
    private ArrayList<ButtonChoice> getButtonChoices(XMessage xMsg) {
    	String goBackText = "Go Back";
        String goToMainMenuText = "Main Menu";

    	ArrayList<ButtonChoice> choices = xMsg.getPayload().getButtonChoices();
    	setAssesmentCharacters();
    	if(choices == null)
    		choices = new ArrayList();

    	choices.forEach(c -> {
    		String[] a = c.getText().split(" ");
    		if(a[0] != null && !a[0].isEmpty()) {
    			String key = a[0].toString();
    			a = Arrays.copyOfRange(a, 1, a.length);
    			String text = String.join(" ", a);

    			log.info("text: "+text);
    			c.setKey(key);
    			c.setText(text.trim());
    			if(c.getBackmenu() == null || c.getBackmenu() != true) {
    				c.setBackmenu(false);
    			}
    		}
    	});
    	xMsg.getPayload().setButtonChoices(choices);

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

    /* Set Assesment Characters in variables */
    public void setAssesmentCharacters() {
    	String envAssesOneLevelUpChar = System.getenv("ASSESSMENT_ONE_LEVEL_UP_CHAR");
        String envAssesGoToStartChar = System.getenv("ASSESSMENT_GO_TO_START_CHAR");

        this.assesOneLevelUpChar = envAssesOneLevelUpChar == "0" || (envAssesOneLevelUpChar != null && !envAssesOneLevelUpChar.isEmpty()) ? envAssesOneLevelUpChar : "#";
        this.assesGoToStartChar = envAssesGoToStartChar == "0" || (envAssesGoToStartChar != null && !envAssesGoToStartChar.isEmpty()) ? envAssesGoToStartChar : "*";
    }

    private Boolean isStylingTagMediaType(StylingTag stylingTag) {
        if(stylingTag.equals(StylingTag.IMAGE) || stylingTag.equals(StylingTag.AUDIO) || stylingTag.equals(StylingTag.VIDEO) || stylingTag.equals(StylingTag.DOCUMENT)
        || stylingTag.equals(StylingTag.IMAGE_URL) || stylingTag.equals(StylingTag.AUDIO_URL) || stylingTag.equals(StylingTag.VIDEO_URL) || stylingTag.equals(StylingTag.DOCUMENT_URL)) {
            return true;
        }
        return false;
    }

    private Boolean isInboundMediaMessage(String mimeType) {
        if (FileUtil.isFileTypeAudio(mimeType) || FileUtil.isFileTypeDocument(mimeType) || FileUtil.isFileTypeImage(mimeType)
                || FileUtil.isFileTypeVideo(mimeType)) {
            return true;
        }
        return false;
    }

    private MessageMedia getInboundMediaMessage(PwaWebMedia pwaWebMedia) {
        MessageMedia media = new MessageMedia();
        media.setText(pwaWebMedia.getFileName());
        media.setUrl(pwaWebMedia.getUrl());
        media.setCategory((MediaCategory) CommonUtils.getMediaCategory(pwaWebMedia.getMimeType()));
        return media;
    }

    private String getUUIDFileName(String fileName){
        if(fileName != null && !fileName.isEmpty()){
            if(fileName.lastIndexOf(".") == -1){
                return null;
            }
            String ext = fileName.substring(fileName.lastIndexOf("."));
            fileName = UUID.randomUUID().toString();
            fileName += ext;
            return fileName;
        }
        return null;
    }

}

