package com.samagra.adapter.cdac;

import com.fasterxml.jackson.databind.ObjectMapper;
import messagerosa.core.model.SenderReceiverInfo;
import messagerosa.core.model.XMessage;
import messagerosa.core.model.XMessagePayload;
import org.junit.jupiter.api.Test;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Unmarshaller;
import java.io.ByteArrayInputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutionException;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CdacBulkClientTest {

//    @Test
//    public void testForOutboundAPICall() throws Exception {
//        SenderReceiverInfo to = SenderReceiverInfo.builder()
//                .userID("9415787824,9673409136")
//                .build();
//        Map<String, String> metadata = new HashMap<>();
//        metadata.put("senderID", "HPGOVT");
//        SenderReceiverInfo from = SenderReceiverInfo.builder()
//                .userID("hpgovt-hpssa")
//                .meta(metadata)
//                .build();
//        XMessagePayload payload = XMessagePayload.builder()
//                .text("This is a test message")
//                .build();
//        XMessage xMessage = XMessage.builder()
//                .app("")
//                .messageState(XMessage.MessageState.NOT_SENT)
//                .messageType(XMessage.MessageType.TEXT)
//                .to(to)
//                .from(from)
//                .payload(payload)
//                .build();
//
//        CDACClient cdacClient = CDACClient.builder()
//                .xMsg(xMessage)
//                .username("hpgovt-hpssa")
//                .password("hpssa@12")
//                .message(payload.getText())
//                .baseURL("https://msdgweb.mgov.gov.in/esms/sendsmsrequest")
//                .build();
//
//        List<String> messageIds = cdacClient.sendBulkSMS();
//
//        System.out.println(messageIds);
//        assertTrue(xMessage.getMessageId().getChannelMessageId().matches("[0-9]+hpgovt-hpssa"));
//    }
//
//    @Test
//    public void testTracking() throws ExecutionException, InterruptedException {
//        CDACClient cdacClient = CDACClient.builder()
//                .username("hpgovt-hpssa")
//                .password("hpssa@12")
//                .baseURL("https://msdgweb.mgov.gov.in/esms/sendsmsrequest")
//                .trackBaseURL("https://msdgweb.mgov.gov.in/XMLForReportG/reportXMLNew")
//                .build();
//        TrackDetails td = cdacClient.trackMultipleMessages("290920201601346533977hpgovt-hpssa,290920201601346533977hpgovt-hpssa,290920201601346533977hpgovt-hpssa");
//        assertEquals(td.getDeliveredSMSCount(), "6");
//    }
//
//    @Test
//    public void testTrackDetailsMerging() throws Exception{
//        String response1 = "<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?><dept value=\"hpgovt-hpssa\"><msgid>290920201601346533977hpgovt-hpssa</msgid><delvSMSCount>2</delvSMSCount><fldSMSCount>0</fldSMSCount><subSMSCount>0</subSMSCount><undelv/><del><no mobNo=\"919415787824\">DELIVRED</no><no mobNo=\"919673409136\">DELIVRED</no></del><sub/><fld/></dept>";
//        String response2 = "<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?><dept value=\"hpgovt-hpssa\"><msgid>290920201601346533977hpgovt-hpssa</msgid><delvSMSCount>2</delvSMSCount><fldSMSCount>0</fldSMSCount><subSMSCount>0</subSMSCount><undelv/><del><no mobNo=\"919415787824\">DELIVRED</no><no mobNo=\"919673409136\">DELIVRED</no></del><sub/><fld/></dept>";
//        JAXBContext context = null;
//        List<TrackDetails> trackDetails = new ArrayList<>();
//        try {
//            context = JAXBContext.newInstance(TrackDetails.class);
//            Unmarshaller jaxbUnmarshaller = context.createUnmarshaller();
//            TrackDetails trackSMSResponse1 = (TrackDetails) jaxbUnmarshaller.unmarshal((new ByteArrayInputStream(response1.getBytes())));
//            TrackDetails trackSMSResponse2 = (TrackDetails) jaxbUnmarshaller.unmarshal((new ByteArrayInputStream(response2.getBytes())));
//            trackDetails.add(trackSMSResponse1);
//            trackDetails.add(trackSMSResponse2);
//
//            CDACClient cdacClient = CDACClient.builder().build();
//            TrackDetails t = cdacClient.mergeTrackDetails(trackDetails);
//            System.out.println(new ObjectMapper().writeValueAsString(t));
//            assertEquals(t.getDeliveredSMSCount(), "4");
//        } catch (JAXBException e) {
//            e.printStackTrace();
//        }
//
//    }

}
