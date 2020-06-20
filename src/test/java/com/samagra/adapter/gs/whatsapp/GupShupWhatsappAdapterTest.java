package com.samagra.adapter.gs.whatsapp;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.samagra.common.Request.GSWhatsAppMessage;
import messagerosa.core.model.XMessage;
import org.junit.jupiter.api.Test;

import javax.xml.bind.JAXBException;

import static org.junit.jupiter.api.Assertions.*;

class GupShupWhatsappAdapterTest {

    @Test
    public void testForSimplePayload() throws JsonProcessingException, JAXBException {
        // Get this from https://www.gupshup.io/developer/docs/bot-platform/guide/whatsapp-api-documentation#IMNE
        // Convert it to string using => https://www.freeformatter.com/json-escape.html#ad-output
        String payload = "{\r\n            \"app\": \"DemoApp\",\r\n                \"timestamp\": 1580227766370,\r\n                \"version\": 2,\r\n                \"type\": \"message\",\r\n                \"payload\": {\r\n            \"id\": \"ABEGkYaYVSEEAhAL3SLAWwHKeKrt6s3FKB0c\",\r\n                    \"source\": \"918x98xx21x4\",\r\n                    \"type\": \"text\",\r\n                    \"payload\": {\r\n                \"text\": \"Hi\"\r\n            },\r\n            \"sender\": {\r\n                \"phone\": \"918x98xx21x4\",\r\n                        \"name\": \"Smit\",\r\n                        \"country_code\": \"91\",\r\n                        \"dial_code\": \"8x98xx21x4\"\r\n            }\r\n        }\r\n        }";
        GSWhatsAppMessage message = new ObjectMapper().readValue(payload, GSWhatsAppMessage.class);
        XMessage xMessage = GupShupWhatsappAdapter.convertMessageToXMsg(message);
        assertEquals(xMessage.getMessageId(), "ABEGkYaYVSEEAhAL3SLAWwHKeKrt6s3FKB0c");
    }

}