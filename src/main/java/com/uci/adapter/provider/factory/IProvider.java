package com.uci.adapter.provider.factory;

import com.fasterxml.jackson.core.JsonProcessingException;
import messagerosa.core.model.XMessage;
import reactor.core.publisher.Mono;

import jakarta.xml.bind.JAXBException;

public interface IProvider {

    public void processOutBoundMessage(XMessage nextMsg) throws Exception;

    public Mono<XMessage> processOutBoundMessageF(XMessage nextMsg) throws Exception;

    public Mono<XMessage> convertMessageToXMsg(Object message) throws JAXBException, JsonProcessingException;
}
