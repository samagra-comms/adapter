package com.samagra.adapter.cdac;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import messagerosa.core.model.SenderReceiverInfo;
import messagerosa.core.model.XMessage;
import messagerosa.core.model.XMessagePayload;
import org.junit.jupiter.api.Test;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Unmarshaller;

import java.io.ByteArrayInputStream;
import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class CdacBulkSmsAdapterTest {

//    @Test
//    public void testForOutboundAPICall() throws Exception {
//        SenderReceiverInfo to = SenderReceiverInfo.builder()
//                .userID("9415787824,9673409136,9958564662")
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
//        CdacBulkSmsAdapter smsAdapter = new CdacBulkSmsAdapter();
//        xMessage = smsAdapter.callOutBoundAPI(xMessage,
//                "https://msdgweb.mgov.gov.in/esms/sendsmsrequest",
//                "hpgovt-hpssa",
//                "hpssa@12");
//
//        System.out.println(xMessage.getMessageId().getChannelMessageId());
//        assertTrue(xMessage.getMessageId().getChannelMessageId().matches("[0-9]+hpgovt-hpssa"));
//    }
//
//    @Test
//    public void testTracking() throws JsonProcessingException {
//        CdacBulkSmsAdapter smsAdapter = new CdacBulkSmsAdapter();
//        String trackResponse = smsAdapter.trackMessage(
//                "hpgovt-hpssa",
//                "hpssa@12",
//                "290920201601346533977hpgovt-hpssa",
//                "https://msdgweb.mgov.gov.in/XMLForReportG/reportXMLNew"
//        );
//
//        JAXBContext context = null;
//        try {
//            context = JAXBContext.newInstance(TrackDetails.class);
//            Unmarshaller jaxbUnmarshaller = context.createUnmarshaller();
//            TrackDetails trackSMSResponse = (TrackDetails) jaxbUnmarshaller.unmarshal((new ByteArrayInputStream(trackResponse.getBytes())));
//        } catch (JAXBException e) {
//            e.printStackTrace();
//        }
//
//        assertEquals("<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?><dept value=\"hpgovt-hpssa\"><msgid>290920201601346533977hpgovt-hpssa</msgid><delvSMSCount>2</delvSMSCount><fldSMSCount>0</fldSMSCount><subSMSCount>0</subSMSCount><undelv/><del><no mobNo=\"919415787824\">DELIVRED</no><no mobNo=\"919673409136\">DELIVRED</no></del><sub/><fld/></dept>", trackResponse);
//    }
//
//    @Test
//    public void marshallTrackCDACBulkSMS() throws Exception{
//        String response = "<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?><dept value=\"hpgovt-hpssa\"><msgid>290920201601346533977hpgovt-hpssa</msgid><delvSMSCount>2</delvSMSCount><fldSMSCount>0</fldSMSCount><subSMSCount>0</subSMSCount><undelv/><del><no mobNo=\"919415787824\">DELIVRED</no><no mobNo=\"919673409136\">DELIVRED</no></del><sub/><fld/></dept>";
//        JAXBContext context = null;
//        try {
//            context = JAXBContext.newInstance(TrackDetails.class);
//            Unmarshaller jaxbUnmarshaller = context.createUnmarshaller();
//            TrackDetails trackSMSResponse = (TrackDetails) jaxbUnmarshaller.unmarshal((new ByteArrayInputStream(response.getBytes())));
//            System.out.println(new ObjectMapper().writeValueAsString(trackSMSResponse));
//        } catch (JAXBException e) {
//            e.printStackTrace();
//        }
//
//    }

}
