package com.uci.adapter.pwa;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectWriter;
import com.uci.adapter.cdn.FileCdnProvider;
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
                .app(webMessage.getAppId())
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

//        xMsg.setMessageId(MessageId.builder().channelMessageId("test").build());
//        xMsg.setMessageState(XMessage.MessageState.SENT);
//        return Mono.just(xMsg);

        return PwaWebService.getInstance().
                sendOutboundMessage(url, outboundMessage)
                .map(new Function<PwaWebResponse, XMessage>() {
            @Override
            public XMessage apply(PwaWebResponse pwaWebResponse) {
                if(pwaWebResponse != null){
                    xMsg.setMessageId(MessageId.builder().channelMessageId(outboundMessage.getMessageId()).build());
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
            xMsg.setMessageId(MessageId.builder().channelMessageId(outboundMessage.getMessageId()).build());
        }
        xMsg.setMessageState(XMessage.MessageState.SENT);
        return xMsg;
    }

    private OutboundMessage getOutboundMessage(XMessage xMsg) throws JAXBException {
        StylingTag stylingTag = xMsg.getPayload().getStylingTag() != null
                ? xMsg.getPayload().getStylingTag() : null;
        PwaMessage pwaMessage = null;
        Boolean plainText = true;

        /* For media */
        if(xMsg.getPayload().getMedia() != null && xMsg.getPayload().getMedia().getUrl() != null) {
            MessageMedia media = xMsg.getPayload().getMedia();
            pwaMessage = PwaMessage.builder()
                    .msg_type(media.getCategory().toString().toUpperCase())
                    .caption(media.getText())
                    .media_url(media.getUrl())
                    .build();
            plainText = false;
        }

        /* For plain text */
        if(plainText) {
            pwaMessage = PwaMessage.builder()
                    .title(getTextMessage(xMsg))
                    .msg_type(StylingTag.TEXT.toString().toUpperCase())
                    .build();
            ArrayList<ButtonChoice> buttonChoices = this.getButtonChoices(xMsg);
            if(buttonChoices.size() > 0) {
                pwaMessage.setChoices(buttonChoices);
            }
        }

        return OutboundMessage.builder()
        		.message(pwaMessage)
				.to(xMsg.getMessageId().getReplyId())
                .messageId(UUID.randomUUID().toString())
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
        media.setCategory(CommonUtils.getMediaCategoryByMimeType(pwaWebMedia.getMimeType()));
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

