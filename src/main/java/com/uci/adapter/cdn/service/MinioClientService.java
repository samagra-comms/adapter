package com.uci.adapter.cdn.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.inversoft.error.Errors;
import com.inversoft.rest.ClientResponse;
import com.uci.adapter.cdn.FileCdnProvider;
import com.uci.utils.cache.service.RedisCacheService;
import io.fusionauth.client.FusionAuthClient;
import io.fusionauth.domain.api.LoginRequest;
import io.fusionauth.domain.api.LoginResponse;
import io.minio.MinioClient;
import io.minio.UploadObjectArgs;
import io.minio.credentials.StaticProvider;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import okhttp3.*;
import org.json.JSONObject;
import org.json.XML;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.util.UriComponents;
import org.springframework.web.util.UriComponentsBuilder;

import java.io.IOException;
import java.net.URI;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

@Setter
@Getter
@Slf4j
@Service
public class MinioClientService implements FileCdnProvider {
    @Value("${cdn.minio.login.id}")
    private String minioLoginId;

    @Value("${cdn.minio.password}")
    private String minioPassword;

    @Value("${cdn.minio.application.id}")
    private String minioAppId;

    @Value("${cdn.minio.bucket.id}")
    private String minioBucketId;

    @Value("${cdn.minio.url}")
    private String minioUrl;

    @Value("${cdn.minio.fa.key}")
    private String minioFAKey;

    @Value("${cdn.minio.fa.url}")
    private String minioFAUrl;

    @Autowired
    private RedisCacheService redisCacheService;
    private FusionAuthClient fusionAuth;
    private LoginRequest loginRequest;

    /**
     * Load default empty object
     */
    private void loadDefaultObjects() {
        log.info("Minio details, loginID: " + minioLoginId + ", password: " + minioPassword + ", appId: " + minioAppId + ", bucketId: " + minioBucketId + ", faKey: " + minioFAKey + ", faUrl: " + minioFAUrl + ", url: " + minioUrl);
        UUID appID = null;
        if (minioAppId != null) {
            appID = UUID.fromString(minioAppId);
        }
        if (this.loginRequest == null) {
            this.loginRequest = new LoginRequest(appID, minioLoginId, minioPassword);
        }
        if (this.fusionAuth == null) {
            this.fusionAuth = new FusionAuthClient(minioFAKey, minioFAUrl);
        }

    }

//    /**
//     * Get File Signed URL from name
//     *
//     * @param name
//     * @return
//     */
//    public String getFileSignedUrl(String name) {
//        String url = "";
//        try {
//            /* Load default objects */
//            loadDefaultObjects();
//
//            MinioClient minioClient = getMinioClient();
//            if (minioClient != null) {
//                try {
//                    url = minioClient.getPresignedObjectUrl(
//                            GetPresignedObjectUrlArgs.builder()
//                                    .method(Method.GET)
//                                    .bucket(this.minioBucketId)
//                                    .object(name)
//                                    .expiry(7, TimeUnit.DAYS)
//                                    .build()
//                    );
//                } catch (InvalidKeyException | InsufficientDataException | InternalException
//                        | InvalidResponseException | NoSuchAlgorithmException | XmlParserException | ServerException
//                        | IllegalArgumentException | IOException e) {
//                    // TODO Auto-generated catch block
//                    log.error("Exception in getCdnSignedUrl: " + e.getMessage());
//                } catch (ErrorResponseException e1) {
//                    log.error("Exception in getFileSignedUrl: " + e1.getMessage() + ", name: " + e1.getClass());
//                }
//            }
//            log.info("minioClient url: " + url);
//        } catch (Exception ex) {
//            log.error("Exception in minio getFileSignedUrl: " + ex.getMessage());
//        }
//
//        return url;
//    }

    /**
     * Get File Signed URL from name
     *
     * @param name of file
     * @return url of image
     */
    public String getFileSignedUrl(String name) {
        if (this.minioUrl.isEmpty() || this.minioBucketId.isEmpty()) {
            log.error("Minio URL or Minio Bucket was null. Minio URL: " + this.minioUrl + ", Minio Bucket: " + this.minioBucketId);
            return "";
        }
        if (name.isEmpty()) {
            log.error("Passed filename was empty.");
            return "";
        }

        // Trim last '/' if present
        if (this.minioUrl.charAt(this.minioUrl.length() - 1) == '/') {
            this.minioUrl = this.minioUrl.substring(0, this.minioUrl.length() - 1);
        }
        if (this.minioBucketId.charAt(this.minioBucketId.length() - 1) == '/') {
            this.minioBucketId = this.minioBucketId.substring(0, this.minioBucketId.length() - 1);
        }

        // For public files {cdn_url}/{bucket}/{filename} can be directly accessed
        return String.format("%s/%s/%s", this.minioUrl, this.minioBucketId, name);
    }

    /**
     * Upload file from file path
     *
     * @param filePath
     * @param name
     * @return
     */
    public String uploadFileFromPath(String filePath, String name) {
        try {
            /* Load default objects */
            loadDefaultObjects();

            MinioClient minioClient = getMinioClient();
            if (minioClient != null) {
                log.info("uploadFileFromPath:: filePath: " + filePath + " Name : " + name);
                minioClient.uploadObject(
                        UploadObjectArgs.builder()
                                .bucket(this.minioBucketId)
                                .object(name)
                                .filename(filePath)
                                .build());

                return getFileSignedUrl(name);
            } else {
                log.error("uploadFileFromPath:: Minio client is null : " + minioClient);
            }
        } catch (Exception ex) {
            log.error("Exception in minio uploadFileFromPath: " + ex.getMessage());
        }

        return "";
    }

    /**
     * Get Minio Client
     *
     * @return
     */
    private MinioClient getMinioClient() {
        if (this.minioUrl != null) {
            try {
                StaticProvider provider = getMinioCredentialsProvider();
                log.info("getMinioClient:: provider: " + provider + ", url: " + this.minioUrl);
                if (provider != null) {
                    return MinioClient.builder()
                            .endpoint(this.minioUrl)
                            .credentialsProvider(provider)
                            .build();
                }
            } catch (Exception e) {
                log.error("Exception in getMinioClient with cache: " + e.getMessage());
            }
        }
        return null;
    }

    /**
     * Get Credentials Provider for Minio Client
     *
     * @return
     */
    private StaticProvider getMinioCredentialsProvider() {
        try {
            String token = getFusionAuthToken();
            log.info("getMinioCredentialsProvider:: token : " + token);
            if (!token.isEmpty()) {
                OkHttpClient client = new OkHttpClient().newBuilder().connectTimeout(90, TimeUnit.SECONDS)
                        .writeTimeout(90, TimeUnit.SECONDS).readTimeout(90, TimeUnit.SECONDS).build();
                MediaType mediaType = MediaType.parse("application/json");

                UriComponents builder = UriComponentsBuilder.fromHttpUrl(this.minioUrl)
                        .queryParam("Action", "AssumeRoleWithWebIdentity")
                        .queryParam("DurationSeconds", 36000) //duration: 10 Hours
                        .queryParam("WebIdentityToken", token)
                        .queryParam("Version", "2011-06-15")
                        .build();
                URI expanded = URI.create(builder.toUriString());
                RequestBody body = RequestBody.create(mediaType, "");
                Request request = new Request.Builder().url(expanded.toString()).method("POST", body)
                        .addHeader("Content-Type", "application/json").build();

                try {
                    Response callResponse = client.newCall(request).execute();
                    String response = callResponse.body().string();
                    log.info("Minio getMinioCredentialsProvider Response::" + response);
                    JSONObject xmlJSONObj = XML.toJSONObject(response);
                    String jsonPrettyPrintString = xmlJSONObj.toString(4);

                    ObjectMapper mapper = new ObjectMapper();
                    JsonNode node = mapper.readTree(jsonPrettyPrintString);
                    JsonNode credentials = node.path("AssumeRoleWithWebIdentityResponse").path("AssumeRoleWithWebIdentityResult").path("Credentials");
                    if (credentials != null && credentials.get("SessionToken") != null
                            && credentials.get("AccessKeyId") != null && credentials.get("SecretAccessKey") != null) {
                        String sessionToken = credentials.get("SessionToken").asText();
                        String accessKey = credentials.get("AccessKeyId").asText();
                        String secretAccessKey = credentials.get("SecretAccessKey").asText();

                        log.info("getMinioCredentialsProvider:: sessionToken: " + sessionToken + ", accessKey: " + accessKey + ", secretAccessKey: " + secretAccessKey);

                        if (!accessKey.isEmpty() && !secretAccessKey.isEmpty() && !sessionToken.isEmpty()) {
                            return new StaticProvider(accessKey, secretAccessKey, sessionToken);
                        }
                    } else {
                        if (node.path("ErrorResponse") != null
                                && node.path("ErrorResponse").path("Error") != null
                                && node.path("ErrorResponse").path("Error").path("Message") != null) {
                            log.error("Error when getting credentials for minio client: " + node.path("ErrorResponse").path("Error").path("Message").asText());
                        }
                    }
                } catch (IOException e) {
                    log.error("IOException in getMinioCredentialsProvider for request call: " + e.getMessage());
                }
            }
        } catch (Exception e) {
            log.error("Exception in getMinioCredentialsProvider: " + e.getMessage());
        }
        return null;
    }

    /**
     * Get Fustion Auth Token
     *
     * @return
     */
    private String getFusionAuthToken() {
        String token = "";
        try {
            ClientResponse<LoginResponse, Errors> clientResponse = this.fusionAuth.login(this.loginRequest);
            if (clientResponse.wasSuccessful()) {
                token = clientResponse.successResponse.token;
            }
        } catch (Exception e) {
            log.error("Exception in getFusionAuthToken: " + e.getMessage());
        }
        return token;
    }
}
