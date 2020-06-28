package com.samagra.adapter.provider.factory;

import com.samagra.common.Request.CommonMessage;
import messagerosa.core.model.XMessage;

import javax.xml.bind.JAXBException;

public abstract class AbstractProvider implements  IProvider{
  public  String channel ="";
  public  String provider = "";
  public   void processInBoundMessage(XMessage nextMsg, XMessage currentMsg) throws Exception{};
  public  void processInBoundMessage(XMessage nextMsg) throws Exception{};
}
