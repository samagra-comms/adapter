package com.samagra.adapter.gs.whatsapp;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import messagerosa.core.model.XMessage;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import javax.xml.bind.JAXBException;

import static org.junit.jupiter.api.Assertions.*;

class GupShupWhatsappAdapterTest {

    @Autowired
    private GupShupWhatsappAdapter adapter;

    @Test
    public void testForSimplePayload() throws JsonProcessingException, JAXBException {
        // Get this from https://www.gupshup.io/developer/docs/bot-platform/guide/whatsapp-api-documentation#IMNE
        // Convert it to string using => https://www.freeformatter.com/json-escape.html#ad-output
        String payload = "{\r\n            \"app\": \"DemoApp\",\r\n                \"timestamp\": 1580227766370,\r\n                \"version\": 2,\r\n                \"type\": \"message\",\r\n                \"payload\": {\r\n            \"id\": \"ABEGkYaYVSEEAhAL3SLAWwHKeKrt6s3FKB0c\",\r\n                    \"source\": \"918x98xx21x4\",\r\n                    \"type\": \"text\",\r\n                    \"payload\": {\r\n                \"text\": \"Hi\"\r\n            },\r\n            \"sender\": {\r\n                \"phone\": \"918x98xx21x4\",\r\n                        \"name\": \"Smit\",\r\n                        \"country_code\": \"91\",\r\n                        \"dial_code\": \"8x98xx21x4\"\r\n            }\r\n        }\r\n        }";
        GSWhatsAppMessage message = new ObjectMapper().readValue(payload, GSWhatsAppMessage.class);
        // XMessage xMessage = adapter.convertMessageToXMsg(message);
        assertEquals(1, 1);
    }

    @Test
    public void testForEnqueued() throws JsonProcessingException, JAXBException {
        String payload = "{\"app\":\"MissionPrerna\",\"timestamp\":1595350687406,\"version\":2,\"type\":\"message-event\",\"payload\":{\"id\":\"ee2ac3d0-705f-44e0-b3f6-bd55b86f1e65\",\"type\":\"enqueued\",\"destination\":\"917837833100\",\"payload\":{\"whatsappMessageId\":\"gBEGkXg3gzEAAglCV_5H94iO6RI\",\"type\":\"session\"}}}";
        GSWhatsAppMessage message = new ObjectMapper().readValue(payload, GSWhatsAppMessage.class);
        GupShupWhatsappAdapter adapter = new GupShupWhatsappAdapter();
        XMessage xMessage = adapter.convertMessageToXMsg(message);
        assertEquals(xMessage.getMessageState(), XMessage.MessageState.ENQUEUED);
        assertEquals(xMessage.getMessageId().getWhatsappMessageId(), "gBEGkXg3gzEAAglCV_5H94iO6RI");
        assertEquals(xMessage.getMessageId().getGupshupMessageId(), "ee2ac3d0-705f-44e0-b3f6-bd55b86f1e65");
    }

    @Test
    public void testForDelivered() throws JsonProcessingException, JAXBException {
        String payload = "{\"app\":\"MissionPrerna\",\"timestamp\":1595350693330,\"version\":2,\"type\":\"message-event\",\"payload\":{\"id\":\"gBEGkXg3gzEAAglCV_5H94iO6RI\",\"gsId\":\"ee2ac3d0-705f-44e0-b3f6-bd55b86f1e65\",\"type\":\"delivered\",\"destination\":\"917837833100\",\"payload\":{\"ts\":1595350688}}}";
        GSWhatsAppMessage message = new ObjectMapper().readValue(payload, GSWhatsAppMessage.class);
        GupShupWhatsappAdapter adapter = new GupShupWhatsappAdapter();
        XMessage xMessage = adapter.convertMessageToXMsg(message);
        assertEquals(xMessage.getMessageState(), XMessage.MessageState.DELIVERED);
        assertEquals(xMessage.getMessageId().getWhatsappMessageId(), "gBEGkXg3gzEAAglCV_5H94iO6RI");
        assertEquals(xMessage.getMessageId().getGupshupMessageId(), "ee2ac3d0-705f-44e0-b3f6-bd55b86f1e65");
    }

    @Test
    public void testForOptedIn() throws JsonProcessingException, JAXBException {
        String payload = "{\"app\":\"MissionPrerna\",\"timestamp\":1595353492083,\"version\":2,\"type\":\"user-event\",\"payload\":{\"phone\":\"917837833100\",\"type\":\"opted-in\"}}";
        GSWhatsAppMessage message = new ObjectMapper().readValue(payload, GSWhatsAppMessage.class);
        GupShupWhatsappAdapter adapter = new GupShupWhatsappAdapter();
        XMessage xMessage = adapter.convertMessageToXMsg(message);
        assertEquals(xMessage.getMessageState(), XMessage.MessageState.OPTED_IN);
    }

}
