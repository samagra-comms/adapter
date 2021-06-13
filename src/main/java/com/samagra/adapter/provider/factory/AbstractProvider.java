package com.samagra.adapter.provider.factory;

import messagerosa.core.model.XMessage;

public abstract class AbstractProvider implements  IProvider{
  public  String channel ="";
  public  String provider = "";

  public  void processOutBoundMessage(XMessage nextMsg) throws Exception{};
}
