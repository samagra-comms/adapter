package com.uci.adapter.sunbird.web;


import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.notNull;

@Slf4j
@ExtendWith(MockitoExtension.class)
public class SunbirdWebPortalAdapterTest {
//    String simplePayload;
//    ObjectMapper objectMapper;
//
//    SunbirdWebPortalAdapter adapter;
//
//    @Mock
//    XMessageRepo xMessageRepo;
//
//    @Mock
//    XMessageDAO xMessageDAO;
//
//    @SneakyThrows
//    @BeforeEach
//    public void init() {
//        objectMapper = new ObjectMapper();
//        simplePayload = "{\"Body\":\"1\",\"userId\":\"2da3ad1ac0422d59ef004fdb173706ed\",\"appId\":\"prod.diksha.portal\",\"channel\":\"ORG_001\",\"From\":\"2da3ad1ac0422d59ef004fdb173706ed\",\"context\":null}";
//        adapter =  SunbirdWebPortalAdapter.builder()
//                .build();
//    }
//
//
//
//    @Test
//    public void simplePayloadParsing() throws JsonProcessingException, JAXBException {
//        ArrayList<XMessageDAO> xMessageDAOArrayList = new ArrayList<>();
//        xMessageDAOArrayList.add(xMessageDAO);
//        SunbirdWebMessage message = objectMapper.readValue(simplePayload, SunbirdWebMessage.class);
//        Mono<XMessage> xMessage = adapter.convertMessageToXMsg(message);
//        StepVerifier.create(xMessage)
//                .consumeNextWith(new Consumer<XMessage>() {
//                    @Override
//                    public void accept(XMessage xMessage) {
//                        assertEquals("2da3ad1ac0422d59ef004fdb173706ed", xMessage.getFrom().getUserID());
//                    }
//                })
//                .expectComplete()
//                .verify();
//    }
}
