package com.uci.adapter.cdn.service;

import com.uci.adapter.cdn.FileCdnProvider;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.sunbird.cloud.storage.BaseStorageService;
import org.sunbird.cloud.storage.factory.StorageConfig;
import org.sunbird.cloud.storage.factory.StorageServiceFactory;
import scala.Option;

import java.io.InputStream;

@Slf4j
@Service
public class SunbirdCloudMediaService implements FileCdnProvider {
    private BaseStorageService service;

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

    /**
     * Load default empty object
     */
    private void loadDefaultObjects () {
        log.info("Sunbird details, type: "+mediaStorageType+", key: "+mediaStorageKey+", secret: "+mediaStorageSecret+", url: "+mediaStorageUrl+", container: "+mediaStorageContainer);
        if(this.service == null && mediaStorageType != null && mediaStorageKey != null && mediaStorageSecret != null && mediaStorageUrl != null) {
            String urlT = mediaStorageUrl;

            StorageConfig config = new StorageConfig(mediaStorageType, mediaStorageKey, this.mediaStorageSecret, getStringObject(urlT));
            BaseStorageService service = StorageServiceFactory.getStorageService(config);
            this.service = service;
        }
    }

    /**
     * Uplaod file from file path
     * @param filePath
     * @return
     */
    public String uploadFileFromPath(String filePath, String name) {
        try{
            /* Load default objects */
            loadDefaultObjects();

            String uploadedFile = this.service.upload(mediaStorageContainer,
                                    filePath,
                                    name,
                                    getBooleanObject(false),
                                    getIntegerObject(1),
                                    getIntegerObject(2),
                                    getIntegerObject(36000));
            return uploadedFile;
        } catch (Exception ex) {
            log.error("Exception in sunbird uploadFileFromUrl: "+ex.getMessage());
        }

        return "";
    }

    /**
     * Get signed url of file by name
     * @param name
     * @return
     */
    public String getFileSignedUrl(String name) {
        try{
            /* Load default objects */
            loadDefaultObjects();

            return this.service.getSignedURL(mediaStorageContainer,
                    name,
                    getIntegerObject(36000),
                    getStringObject("r")
            );
        } catch(Exception ex) {
            log.error("Exception in sunbird getFileSignedUrl: "+ex.getMessage());
        }
        return "";
    }

    private Option<Object> getBooleanObject(Boolean value) {
        return new Option<Object>() {
            @Override
            public boolean isEmpty() {
                return false;
            }

            @Override
            public Object get() {
                return value;
            }

            @Override
            public Object productElement(int n) {
                return null;
            }

            @Override
            public int productArity() {
                return 0;
            }

            @Override
            public boolean canEqual(Object that) {
                return false;
            }
        };
    }

    private Option<Object> getIntegerObject(Integer value) {
        return new Option<Object>() {
            @Override
            public boolean isEmpty() {
                return false;
            }

            @Override
            public Object get() {
                return value;
            }

            @Override
            public Object productElement(int n) {
                return null;
            }

            @Override
            public int productArity() {
                return 0;
            }

            @Override
            public boolean canEqual(Object that) {
                return false;
            }
        };
    }

    private Option<String> getStringObject(String value) {
        return new Option<String>() {
            @Override
            public boolean isEmpty() {
                return false;
            }

            @Override
            public String get() {
                return value;
            }

            @Override
            public Object productElement(int n) {
                return null;
            }

            @Override
            public int productArity() {
                return 0;
            }

            @Override
            public boolean canEqual(Object that) {
                return false;
            }
        };
    }
}
