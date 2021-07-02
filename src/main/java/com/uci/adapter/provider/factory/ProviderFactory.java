package com.uci.adapter.provider.factory;

import com.uci.adapter.gs.whatsapp.GupShupWhatsappAdapter;
import com.uci.adapter.netcore.whatsapp.NetcoreWhatsappAdapter;
import com.uci.dao.repository.XMessageRepository;
import com.uci.utils.BotService;
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
    public XMessageRepository xmsgRepo;

    @Autowired
    public BotService botService;

    public IProvider getProvider(String provider,String channel) {
        if (provider.toLowerCase().equals("gupshup") && channel.toLowerCase().equals("whatsapp")) {
            GupShupWhatsappAdapter gupshupWhatsapp = GupShupWhatsappAdapter
                    .builder()
                    .botservice(botService)
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
                    .botservice(botService)
                    .build();
            return netcoreWhatsappAdapter;
        }
        return null;
    }
}