package com.samagra.adapter.sunbird.web;


import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.samagra.adapter.sunbird.web.inbound.SunbirdWebMessage;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import messagerosa.core.model.XMessage;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import javax.xml.bind.JAXBException;
import java.util.function.Consumer;

import static org.junit.jupiter.api.Assertions.assertEquals;

@Slf4j
@ExtendWith(MockitoExtension.class)
public class SunbirdWebPortalAdapterTest {
    String simplePayload;
    ObjectMapper objectMapper;

    SunbirdWebPortalAdapter adapter;

    @SneakyThrows
    @BeforeEach
    public void init() {
        objectMapper = new ObjectMapper();
        simplePayload = "{\"Body\":\"1\",\"userId\":\"2da3ad1ac0422d59ef004fdb173706ed\",\"appId\":\"prod.diksha.portal\",\"channel\":\"ORG_001\",\"From\":\"2da3ad1ac0422d59ef004fdb173706ed\",\"context\":null}";
        adapter =  SunbirdWebPortalAdapter.builder()
                .build();
    }



    @Test
    public void simplePayloadParsing() throws JsonProcessingException, JAXBException {
        SunbirdWebMessage message = objectMapper.readValue(simplePayload, SunbirdWebMessage.class);
        Mono<XMessage> xMessage = adapter.convertMessageToXMsg(message);
        StepVerifier.create(xMessage)
                .consumeNextWith(new Consumer<XMessage>() {
                    @Override
                    public void accept(XMessage xMessage) {
                        assertEquals("2da3ad1ac0422d59ef004fdb173706ed", xMessage.getFrom().getUserID());
                    }
                })
                .expectComplete()
                .verify();
    }
}
