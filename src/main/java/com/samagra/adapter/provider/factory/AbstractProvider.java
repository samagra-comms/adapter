package com.samagra.adapter.provider.factory;

import messagerosa.core.model.XMessage;

public abstract class AbstractProvider implements  IProvider{
  public   void processInBoundMessage(XMessage nextMsg, XMessage currentMsg) throws Exception{};
  public  void processInBoundMessage(XMessage nextMsg) throws Exception{};
}
