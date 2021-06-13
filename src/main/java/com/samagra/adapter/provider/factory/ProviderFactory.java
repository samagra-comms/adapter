package com.samagra.adapter.provider.factory;

import com.samagra.adapter.gs.whatsapp.GupShupWhatsappAdapter;
import com.samagra.adapter.netcore.whatsapp.NetcoreWhatsappAdapter;
import com.samagra.user.BotService;
import messagerosa.dao.XMessageRepo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

@Component
public class ProviderFactory {

    @Autowired
    @Qualifier("gupshupSMSAdapter")
    private IProvider gupshupSMS;

    @Autowired
    @Qualifier("cdacSMSBulkAdapter")
    private IProvider cdacSMSBulk;

    @Autowired
    public XMessageRepo xmsgRepo;

    public IProvider getProvider(String provider,String channel) {
        if (provider.toLowerCase().equals("gupshup") && channel.toLowerCase().equals("whatsapp")) {
            GupShupWhatsappAdapter gupshupWhatsapp = GupShupWhatsappAdapter
                    .builder()
                    .botservice(new BotService())
                    .xmsgRepo(xmsgRepo)
                    .build();
            return gupshupWhatsapp;
        }else if (provider.equals("gupshup") && channel.equals("sms")) {
            return gupshupSMS;
        } else if(provider.equals("cdac") && channel.toLowerCase().equals("sms")){
            return cdacSMSBulk;
        }else if(provider.equalsIgnoreCase("Netcore") && channel.toLowerCase().equalsIgnoreCase("whatsapp")){
            NetcoreWhatsappAdapter netcoreWhatsappAdapter = NetcoreWhatsappAdapter
                    .builder()
                    .botservice(new BotService())
                    .xmsgRepo(xmsgRepo)
                    .build();
            return netcoreWhatsappAdapter;
        }
        return null;
    }
}
