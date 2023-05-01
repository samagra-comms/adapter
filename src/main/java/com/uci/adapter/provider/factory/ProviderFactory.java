package com.uci.adapter.provider.factory;

import com.uci.adapter.cdn.FileCdnFactory;
import com.uci.adapter.firebase.web.FirebaseNotificationAdapter;
import com.uci.adapter.gs.whatsapp.GupShupWhatsappAdapter;
import com.uci.adapter.netcore.whatsapp.NetcoreWhatsappAdapter;
import com.uci.adapter.pwa.PwaWebPortalAdapter;
import com.uci.adapter.sunbird.web.SunbirdWebPortalAdapter;
import com.uci.adapter.utils.CommonUtils;
import com.uci.dao.repository.XMessageRepository;
import com.uci.utils.BotService;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
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
    public FileCdnFactory fileCdnFactory;

    @Autowired
    public CommonUtils commonUtils;

	@Autowired
	private NetcoreWhatsappAdapter netcoreWhatsappAdapter;

    @Value("${fcm.notificationKeyEnable:#{'true'}}")
    private String notificationKeyEnable;

    public IProvider getProvider(String provider,String channel) {
        if (provider.toLowerCase().equals("gupshup") && channel.toLowerCase().equals("whatsapp")) {
            GupShupWhatsappAdapter gupshupWhatsapp = GupShupWhatsappAdapter
                    .builder()
                    .botservice(botService)
                    .fileCdnProvider(fileCdnFactory.getFileCdnProvider())
                    .xmsgRepo(xmsgRepo)
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
            PwaWebPortalAdapter pwaAdapter = PwaWebPortalAdapter.builder()
                    .commonUtils(commonUtils)
                    .fileCdnProvider(fileCdnFactory.getFileCdnProvider())
                    .build();
            return pwaAdapter;
        } else if(provider.equalsIgnoreCase("Netcore") && channel.toLowerCase().equalsIgnoreCase("whatsapp")){
			netcoreWhatsappAdapter.setBotservice(botService);
			netcoreWhatsappAdapter.setFileCdnProvider(fileCdnFactory.getFileCdnProvider());
            return netcoreWhatsappAdapter;
        } else if(provider.toLowerCase().equals("firebase") && channel.toLowerCase().equals("web")){
            return FirebaseNotificationAdapter.builder().botService(botService).notificationKeyEnable(notificationKeyEnable).build();
        }
        return null;
    }
    
}
