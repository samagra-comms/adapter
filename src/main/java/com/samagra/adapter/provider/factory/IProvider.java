package com.samagra.adapter.provider.factory;

import com.samagra.common.Request.GSWhatsAppMessage;
import messagerosa.core.model.XMessage;

import javax.xml.bind.JAXBException;

public interface IProvider {

      public   void processInBoundMessage(XMessage nextMsg, XMessage currentMsg) throws Exception;
     public   void processInBoundMessage(XMessage nextMsg) throws Exception;
     public XMessage callOutBoundAPI(XMessage xMsg) throws Exception ;
    public XMessage convertMessageToXMsg(GSWhatsAppMessage message) throws JAXBException ;}