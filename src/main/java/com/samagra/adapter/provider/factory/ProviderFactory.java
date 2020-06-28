package com.samagra.adapter.provider.factory;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

@Component
public class ProviderFactory {

    @Autowired
    @Qualifier("gupshupWhatsappAdapter")
    private IProvider gupshupWhatsapp;

    @Autowired
    @Qualifier("gupshupSMSAdapter")
    private IProvider gupshupSMS;

    public IProvider getProvider(String provider,String channel) {
        if (provider.equals("gupshup") && channel.equals("whatsapp")) {
            return gupshupWhatsapp;
        }else if (provider.equals("gupshup") && channel.equals("sms")) {
            return gupshupSMS;
        }
        return null;
    }
}
