package com.uci.adapter.provider.factory;

import com.uci.adapter.firebase.web.FirebaseNotificationAdapter;
import com.uci.adapter.gs.whatsapp.GupShupWhatsappAdapter;
import com.uci.adapter.netcore.whatsapp.NetcoreWhatsappAdapter;
import com.uci.adapter.pwa.PwaWebPortalAdapter;
import com.uci.adapter.service.media.SunbirdCloudMediaService;
import com.uci.adapter.sunbird.web.SunbirdWebPortalAdapter;
import com.uci.adapter.utils.CommonUtils;
import com.uci.dao.repository.XMessageRepository;
import com.uci.utils.BotService;
import com.uci.utils.azure.AzureBlobService;
import com.uci.utils.cdn.FileCdnFactory;
import com.uci.utils.cdn.samagra.MinioClientService;

import com.uci.utils.service.VaultService;
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
    public AzureBlobService azureBlobService;

    @Autowired
    public MinioClientService minioClientService;

    @Autowired
    public FileCdnFactory fileCdnFactory;

    @Autowired
    public VaultService vaultService;

    @Autowired
    public CommonUtils commonUtils;

    @Value("${sunbird.cloud.media.storage.type}")
    private String mediaStorageType;

    @Value("${sunbird.cloud.media.storage.key}")
    private String mediaStorageKey;

    @Value("${sunbird.cloud.media.storage.secret}")
    private String mediaStorageSecret;

    @Value("${sunbird.cloud.media.storage.url}")
    private String mediaStorageUrl;

    @Value("${sunbird.cloud.media.storage.container}")
    private String mediaStorageContainer;

    public IProvider getProvider(String provider,String channel) {
        SunbirdCloudMediaService mediaService = new SunbirdCloudMediaService(mediaStorageType, mediaStorageKey, mediaStorageSecret, mediaStorageUrl, mediaStorageContainer);
        if (provider.toLowerCase().equals("gupshup") && channel.toLowerCase().equals("whatsapp")) {
            GupShupWhatsappAdapter gupshupWhatsapp = GupShupWhatsappAdapter
                    .builder()
                    .botservice(botService)
                    .fileCdnProvider(fileCdnFactory.getFileCdnProvider())
                    .xmsgRepo(xmsgRepo)
                    .mediaService(mediaService)
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
            NetcoreWhatsappAdapter netcoreWhatsappAdapter = NetcoreWhatsappAdapter
                    .builder()
                    .botservice(botService)
                    .fileCdnProvider(fileCdnFactory.getFileCdnProvider())
                    .build();
            return netcoreWhatsappAdapter;
        } else if(provider.toLowerCase().equals("firebase") && channel.toLowerCase().equals("web")){
            return FirebaseNotificationAdapter.builder().botService(botService).vaultService(vaultService).build();
        }
        return null;
    }
    
}
