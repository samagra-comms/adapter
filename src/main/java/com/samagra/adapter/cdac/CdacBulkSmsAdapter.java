package com.samagra.adapter.cdac;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.samagra.adapter.provider.factory.AbstractProvider;
import com.samagra.adapter.provider.factory.IProvider;
import com.samagra.user.CampaignService;
import io.fusionauth.domain.Application;
import lombok.extern.slf4j.Slf4j;
import messagerosa.core.model.MessageId;
import messagerosa.core.model.SenderReceiverInfo;
import messagerosa.core.model.XMessage;
import messagerosa.dao.XMessageDAO;
import messagerosa.dao.XMessageDAOUtills;
import messagerosa.dao.XMessageRepo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;
import reactor.core.publisher.Flux;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Unmarshaller;
import java.io.ByteArrayInputStream;
import java.net.URI;
import java.util.List;

@Slf4j
@Qualifier("cdacSMSBulkAdapter")
@Service
public class CdacBulkSmsAdapter extends AbstractProvider implements IProvider {

    @Value("${provider.CDAC.SMS.username}")
    private String username;

    @Value("${provider.CDAC.SMS.password}")
    private String password;

    private final static String OUTBOUND = "https://msdgweb.mgov.gov.in/esms/sendsmsrequest";
    private final static String TRACK_BASE_URL = "https://msdgweb.mgov.gov.in/XMLForReportG/reportXMLNew";

    @Autowired
    @Qualifier("rest")
    private RestTemplate restTemplate;

    @Autowired
    public XMessageRepo xmsgRepo;

    @Override
    public XMessage convertMessageToXMsg(Object msg) throws JsonProcessingException {

        // Build xMessage => Most calls would be to update the status of Messages
        return XMessage.builder().build();
    }

    /**
     * @param from: User form the whom the message was received.
     * @param text: User's text
     * @return appName
     */
    private String getAppName(SenderReceiverInfo from, String text) {
        String appName;
        try {
            appName = (String) CampaignService.getCampaignFromStartingMessage(text).data.get("appName");
            return appName;
        } catch (Exception e) {
            XMessageDAO xMessageLast = xmsgRepo.findTopByUserIdAndMessageStateOrderByTimestampDesc(from.getUserID(), "REPLIED");
            return xMessageLast.getApp();
        }
    }

    @Override
    public void processInBoundMessage(XMessage nextMsg) throws Exception {
        XMessage xMsg = callOutBoundAPI(nextMsg, OUTBOUND, username, password);
        XMessageDAO dao = XMessageDAOUtills.convertXMessageToDAO(xMsg);
        xmsgRepo.save(dao);
    }

    @Override
    public Flux<Boolean> processInBoundMessageFlux(XMessage nextMsg) throws Exception {
        return null;
    }

    public static String trackMessage(String username, String password, String messageID, String baseURL) {
        // track the message and sends response to inbound.
        UriComponentsBuilder builder = UriComponentsBuilder.fromHttpUrl(baseURL)
                .queryParam("password", password)
                .queryParam("userid", username)
                .queryParam("msgid", messageID);

        URI expanded = URI.create(builder.toUriString());
        RestTemplate restTemplate = new RestTemplate();
        return restTemplate.getForObject(expanded, String.class);
    }

    public TrackDetails trackAndUpdate(XMessageDAO xMessageDAO) {
        CDACClient cdacClient = CDACClient.builder()
                .batchSize(20)
                .username(username)
                .password(password)
                .trackBaseURL(TRACK_BASE_URL)
                .build();
        TrackDetails trackDetails = null;
        try {
            trackDetails = cdacClient.trackMultipleMessages(xMessageDAO.getMessageId());
            xMessageDAO.setAuxData(trackDetails.toString());
            xmsgRepo.save(xMessageDAO);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return trackDetails;
    }

    public TrackDetails getLastTrackingReport(String campaignID) throws Exception {
        Application campaign = CampaignService.getCampaignFromID(campaignID);
        String appName = (String) campaign.data.get("appName");
        XMessageDAO xMessage = xmsgRepo.findFirstByAppOrderByTimestampDesc(appName);
        return trackAndUpdate(xMessage);
        /*
        try {
            JAXBContext context = JAXBContext.newInstance(TrackDetails.class);
            Unmarshaller jaxbUnmarshaller = context.createUnmarshaller();
            TrackDetails trackDetails = (TrackDetails) jaxbUnmarshaller.unmarshal((new ByteArrayInputStream(xMessage.getAuxData().getBytes())));
            return trackDetails;
        } catch (Exception e) {
            e.printStackTrace();
            trackAndUpdate(xMessage);
            return null;
        }
         */
    }

    static XMessage callOutBoundAPI(XMessage xMsg, String baseURL, String username, String password) throws Exception {

        String message = xMsg.getPayload().getText();
        StringBuilder finalmessage = new StringBuilder();
        message = message.trim();
        for (int i = 0; i < message.length(); i++) {
            char ch = message.charAt(i);
            int j = (int) ch;
            String sss = "&#" + j + ";";
            finalmessage.append(sss);
        }

        CDACClient cdacClient = CDACClient.builder()
                .xMsg(xMsg)
                .batchSize(20)
                .username(username)
                .password(password)
                .message(finalmessage.toString())
                .baseURL(baseURL)
                .trackBaseURL(TRACK_BASE_URL)
                .build();

        //TEST
        /*
        cdacClient.trackMessage("290920201601346533977hpgovt-hpssa");
        cdacClient.trackMessage("290920201601346533977hpgovt-hpssa");
        cdacClient.trackMessage("290920201601346533977hpgovt-hpssa");
        cdacClient.trackMessage("290920201601346533977hpgovt-hpssa");
        */

        List<String> messageIds = cdacClient.sendBulkSMS();

        if (messageIds.size() == 0) {
            throw new Exception("No messageID found. Response => ");
        } else {
            StringBuilder sb = new StringBuilder();
            String prefix = "";
            for (String messageId : messageIds) {
                sb.append(prefix);
                prefix = ",";
                sb.append(messageId);
            }

            xMsg.setMessageId(MessageId.builder().channelMessageId(sb.toString()).build());
            SenderReceiverInfo to = xMsg.getTo();
            to.setUserID("Bulk");
            xMsg.setTo(to);
            return xMsg;
        }
    }

}
