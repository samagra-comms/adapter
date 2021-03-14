package com.samagra.adapter.provider.factory;

import com.fasterxml.jackson.core.JsonProcessingException;
import messagerosa.core.model.XMessage;
import reactor.core.publisher.Flux;

import javax.xml.bind.JAXBException;

public interface IProvider {

    public void processInBoundMessage(XMessage nextMsg, XMessage currentMsg) throws Exception;

    public void processInBoundMessage(XMessage nextMsg) throws Exception;

    public Flux<Boolean> processInBoundMessageFlux(XMessage nextMsg) throws Exception;

    public XMessage convertMessageToXMsg(Object message) throws JAXBException, JsonProcessingException;
}
