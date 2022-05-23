package com.uci.adapter.provider.factory;

import com.uci.adapter.gs.whatsapp.GupShupWhatsappAdapter;
import com.uci.adapter.netcore.whatsapp.NetcoreWhatsappAdapter;
import com.uci.adapter.pwa.PwaWebPortalAdapter;
import com.uci.adapter.sunbird.web.SunbirdWebPortalAdapter;
import com.uci.dao.repository.XMessageRepository;
import com.uci.utils.BotService;
import com.uci.utils.azure.AzureBlobService;
import com.uci.utils.cdn.samagra.MinioClientService;

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
    
    @Autowired
    public AzureBlobService azureBlobService;

    @Autowired
    public MinioClientService minioClientService;

    public IProvider getProvider(String provider,String channel) {
        if (provider.toLowerCase().equals("gupshup") && channel.toLowerCase().equals("whatsapp")) {
            GupShupWhatsappAdapter gupshupWhatsapp = GupShupWhatsappAdapter
                    .builder()
                    .botservice(botService)
                    .xmsgRepo(xmsgRepo)
                    .azureBlobService(azureBlobService)
                    .minioClientService(minioClientService)
                    .build();
            return gupshupWhatsapp;
        } else if (provider.equals("gupshup") && channel.equals("sms")) {
            return gupshupSMS;
        } else if(provider.equals("cdac") && channel.toLowerCase().equals("sms")){
            return cdacSMSBulk;
        } else if(provider.equals("sunbird") && channel.toLowerCase().equals("web")){
        	SunbirdWebPortalAdapter sunbirdAdapter = SunbirdWebPortalAdapter.builder().build();
            return sunbirdAdapter;
        } else if(provider.equals("pwa") && channel.toLowerCase().equals("web")){
            PwaWebPortalAdapter pwaAdapter = PwaWebPortalAdapter.builder().build();
            return pwaAdapter;
        } else if(provider.equalsIgnoreCase("Netcore") && channel.toLowerCase().equalsIgnoreCase("whatsapp")){
            NetcoreWhatsappAdapter netcoreWhatsappAdapter = NetcoreWhatsappAdapter
                    .builder()
                    .botservice(botService)
                    .azureBlobService(azureBlobService)
                    .build();
            return netcoreWhatsappAdapter;
        }
        return null;
    }
}
