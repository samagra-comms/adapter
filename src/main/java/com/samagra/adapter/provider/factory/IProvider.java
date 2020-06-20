package com.samagra.adapter.provider.factory;

import messagerosa.core.model.XMessage;

public interface IProvider {
      public   void processInBoundMessage(XMessage nextMsg, XMessage currentMsg) throws Exception;
     public   void processInBoundMessage(XMessage nextMsg) throws Exception;
}
