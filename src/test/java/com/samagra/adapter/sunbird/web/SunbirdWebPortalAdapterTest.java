package com.samagra.adapter.sunbird.web;


import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.samagra.adapter.sunbird.web.inbound.SunbirdWebMessage;
import com.uci.utils.BotService;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import messagerosa.core.model.XMessage;
import messagerosa.dao.XMessageDAO;
import messagerosa.dao.XMessageRepo;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import javax.xml.bind.JAXBException;

import java.util.ArrayList;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.notNull;
import static org.mockito.Mockito.when;

@Slf4j
@ExtendWith(MockitoExtension.class)
public class SunbirdWebPortalAdapterTest {
    String simplePayload;
    ObjectMapper objectMapper;

    SunbirdWebPortalAdapter adapter;
    @Mock
    BotService botService;

    @Mock
    XMessageRepo xMessageRepo;

    @Mock
    XMessageDAO xMessageDAO;

    @SneakyThrows
    @BeforeEach
    public void init() {
        when(botService.getCurrentAdapter(any())).thenReturn("A");
        when(botService.getCampaignFromStartingMessage(any())).thenReturn("test");

        objectMapper = new ObjectMapper();
        simplePayload = "{\"Body\":\"1\",\"userId\":\"2da3ad1ac0422d59ef004fdb173706ed\",\"appId\":\"prod.diksha.portal\",\"channel\":\"ORG_001\",\"From\":\"2da3ad1ac0422d59ef004fdb173706ed\",\"context\":null}";
        adapter =  SunbirdWebPortalAdapter.builder()
                .botservice(botService)
                .xmsgRepo(xMessageRepo)
                .build();
    }



    @Test
    public void simplePayloadParsing() throws JsonProcessingException, JAXBException {
        ArrayList<XMessageDAO> xMessageDAOArrayList = new ArrayList<>();
        xMessageDAOArrayList.add(xMessageDAO);
        when(xMessageRepo.findAllByUserIdOrderByTimestamp((String) notNull())).thenReturn(xMessageDAOArrayList);
        SunbirdWebMessage message = objectMapper.readValue(simplePayload, SunbirdWebMessage.class);
        XMessage xMessage = adapter.convertMessageToXMsg(message);
        assertEquals("2da3ad1ac0422d59ef004fdb173706ed", xMessage.getFrom().getUserID());
    }
}
