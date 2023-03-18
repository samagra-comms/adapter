package com.uci.adapter.cdn.service;

import com.azure.storage.blob.BlobClient;
import com.azure.storage.blob.BlobContainerClient;
import com.azure.storage.blob.BlobServiceClient;
import com.azure.storage.blob.BlobServiceClientBuilder;
import com.azure.storage.blob.sas.BlobContainerSasPermission;
import com.azure.storage.blob.sas.BlobSasPermission;
import com.azure.storage.blob.sas.BlobServiceSasSignatureValues;
import com.uci.adapter.cdn.FileCdnProvider;
import com.uci.utils.bot.util.FileUtil;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.MimeTypeUtils;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.time.OffsetDateTime;
import java.util.Random;
import java.util.UUID;

@Setter
@Getter
@Slf4j
@Service
public class AzureBlobService implements FileCdnProvider {
    @Value("${spring.azure.blob.store.account.name}")
    private String azureAccountName;

    @Value("${spring.azure.blob.store.account.key}")
    private String azureAccountKey;

    @Value("${spring.azure.blob.store.container.name}")
    private String azureContainer;

    private BlobServiceClient serviceClient;
    private BlobContainerClient containerClient;

    /**
     * Load default empty object
     */
    public void loadDefaultObjects() {
        log.info("Azure details, accountName: "+azureAccountName+", key: "+azureAccountKey+", container: "+azureContainer);
        String connectionStr = "DefaultEndpointsProtocol=https;AccountName="+azureAccountName+";AccountKey="+azureAccountKey+";EndpointSuffix=core.windows.net";
        if(this.serviceClient == null) {
            this.serviceClient = new BlobServiceClientBuilder().connectionString(connectionStr).buildClient();
        }
        if(this.containerClient == null) {
            this.containerClient = serviceClient.getBlobContainerClient(azureContainer);
        }
    }

    /**
     * Get File signed url from name
     *
     * @param name
     * @return
     */
    public String getFileSignedUrl(String name) {
        try {
            /* Load default objects */
            loadDefaultObjects();

            if(this.containerClient != null) {
                BlobClient blobClient = containerClient.getBlobClient(name);
                log.info("getBlobUrl: " + blobClient.getBlobUrl());

                if (blobClient != null && blobClient.getBlobUrl() != null) {
                    return blobClient.getBlobUrl() + "?" + generateBlobSASToken(blobClient);
                }
            }
        } catch (Exception e) {
            log.error("Exception in azure getFileSignedUrl: "+e.getMessage());
        }
        return "";
    }

    /**
     * Upload File from file path to Azure Blob Storage
     *
     * @param filePath
     * @param name
     */
    public String uploadFileFromPath(String filePath, String name) {
        try{
            /* Load default objects */
            loadDefaultObjects();

            if(this.containerClient != null) {
                // Get a reference to a blob
                BlobClient blobClient = containerClient.getBlobClient(name);
                // Upload the blob
                blobClient.uploadFromFile(filePath);

                // Return blob name
                return getFileSignedUrl(blobClient.getBlobName());
            }
        } catch(Exception ex) {
            ex.printStackTrace();
            log.error("Exception in azure uploadFileFromPath: "+ex.getMessage());
        }

        return "";
    }

    /**
     * Generate SAS token
     * @param blobClient
     * @return
     */
    public String generateBlobSASToken(BlobClient blobClient) {
        // Generate a sas using a blob client
        OffsetDateTime expiryTime = OffsetDateTime.now().plusMonths(1);
        BlobSasPermission blobSasPermission = new BlobSasPermission().setReadPermission(true);
        BlobServiceSasSignatureValues serviceSasValues = new BlobServiceSasSignatureValues(expiryTime,
                blobSasPermission);

        return blobClient.generateSas(serviceSasValues);
    }

    /**
     * Generate SAS token
     * @return
     */
    public String generateContainerSASToken() {
        // Generate a sas using a blob client
        OffsetDateTime expiryTime = OffsetDateTime.now().plusMonths(3);
        // Generate a sas using a container client
        BlobContainerSasPermission containerSasPermission = new BlobContainerSasPermission()
                .setCreatePermission(true)
                .setAddPermission(true)
                .setListPermission(true)
                .setWritePermission(true)
                .setReadPermission(true);
        BlobServiceSasSignatureValues serviceSasValues =
                new BlobServiceSasSignatureValues(expiryTime, containerSasPermission);
        return containerClient.generateSas(serviceSasValues);
    }
}
