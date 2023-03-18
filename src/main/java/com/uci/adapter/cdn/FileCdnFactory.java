package com.uci.adapter.cdn;

import com.uci.adapter.cdn.service.AzureBlobService;
import com.uci.adapter.cdn.service.MinioClientService;
import com.uci.adapter.cdn.service.SunbirdCloudMediaService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class FileCdnFactory {
    @Autowired
    MinioClientService minioClientService;

    @Autowired
    AzureBlobService azureBlobService;

    @Autowired
    SunbirdCloudMediaService sunbirdCloudMediaService;

    public FileCdnProvider getFileCdnProvider() {
        String selected = System.getenv("SELECTED_FILE_CDN");
        if(selected != null && selected.equals("sunbird")) {
            return sunbirdCloudMediaService;
        } else if(selected != null && selected.equals("minio")) {
            return minioClientService;
        } else {
            return azureBlobService;
        }
    }
}
