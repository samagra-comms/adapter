package com.uci.adapter.service.media;

import lombok.extern.slf4j.Slf4j;
import org.sunbird.cloud.storage.BaseStorageService;
import org.sunbird.cloud.storage.factory.StorageConfig;
import org.sunbird.cloud.storage.factory.StorageServiceFactory;
import scala.Option;

import java.time.LocalDateTime;

@Slf4j
public class SunbirdCloudMediaService {
    BaseStorageService service;
    String storageType;
    String storageKey;
    String storageSecret;
    String storageUrl;
    String defaultcontainer;

    public SunbirdCloudMediaService(String storageType, String storageKey, String storageSecret, String storageUrl, String storageContainer) {
        this.storageType = storageType;
        this.storageKey = storageKey;
        this.storageSecret = storageSecret;
        this.storageUrl = storageUrl;
        this.defaultcontainer = storageContainer;
    }

    private BaseStorageService getServiceObject () {
        if(!this.storageType.isEmpty() && !this.storageKey.isEmpty() && !this.storageSecret.isEmpty() && !this.storageUrl.isEmpty()) {
            String urlT = this.storageUrl;
            Option<String> url = new Option<String>() {
                @Override
                public boolean isEmpty() {
                    return false;
                }

                @Override
                public String get() { return urlT; }

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

            StorageConfig config = new StorageConfig(this.storageType, this.storageKey, this.storageSecret, url);
            BaseStorageService service = StorageServiceFactory.getStorageService(config);
            return service;
        }
        return null;
    }

    /**
     * Uplaod file from url
     * @param container
     * @param filePath
     * @return
     */
    public String uploadFileFromPath(String container, String filePath) {
        Option<Object> isDirectory = getIsDirectory(false);
        Option<Object> attempt = getTtl(1);
        Option<Object> retry = getTtl(2);
        Option<Object> ttl = getTtl(36000);

        try{
            if(container == null || container.isEmpty()) {
                container = this.defaultcontainer;
            }
            String uploadedFile = getServiceObject().upload(container, filePath,
                        LocalDateTime.now().toString(), isDirectory, attempt, retry, ttl);
            return uploadedFile;
        } catch (Exception ex) {
            log.error("Exception in uploadFileFromUrl: "+ex.getMessage());
        }

        return "";
    }

    public String getFileSignedUrl() {
        return "";
    }

    private Option<Object> getIsDirectory(Boolean isDirectory) {
        return new Option<Object>() {
            @Override
            public boolean isEmpty() {
                return false;
            }

            @Override
            public Object get() {
                return isDirectory;
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

    private Option<Object> getAttempt(Integer attempt) {
        return new Option<Object>() {
            @Override
            public boolean isEmpty() {
                return false;
            }

            @Override
            public Object get() {
                return attempt;
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

    private Option<Object> getRetry(Integer retry) {
        return new Option<Object>() {
            @Override
            public boolean isEmpty() {
                return false;
            }

            @Override
            public Object get() {
                return retry;
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

    private Option<Object> getTtl(Integer ttl) {
        return new Option<Object>() {
            @Override
            public boolean isEmpty() {
                return false;
            }

            @Override
            public Object get() {
                return ttl;
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
