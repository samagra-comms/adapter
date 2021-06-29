package com.samagra.adapter.sunbird.web;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.samagra.adapter.sunbird.web.outbound.OutboundMessage;
import com.samagra.adapter.sunbird.web.outbound.SunbirdWebResponse;
import okhttp3.*;

import java.io.IOException;

public class SunbirdWebService {
    private OkHttpClient client;
    private MediaType mediaType;
    private String baseURL;
    private SunbirdCredentials credentials;

    public SunbirdWebService(SunbirdCredentials credentials,String baseURL){
        this.client = new OkHttpClient().newBuilder().build();
        this.mediaType = MediaType.parse("application/json");
        this.baseURL = baseURL;
        this.credentials = credentials;
    }

    public SunbirdWebResponse sendText(OutboundMessage message){
        ObjectMapper mapper = new ObjectMapper();
        RequestBody body = null;
        try {
            body = RequestBody.create(mediaType,  mapper.writeValueAsString(message));
            Request request = new Request.Builder()
                    .url(baseURL )
                    .method("POST", body)
                    .addHeader("Content-Type", "application/json")
                    .addHeader("Authorization", "Bearer " + credentials.getToken())
                    .build();
            Response response = client.newCall(request).execute();
            String json = response.body().string();
            return mapper.readValue(json, SunbirdWebResponse.class);
        } catch (JsonProcessingException e) {
            e.printStackTrace();
            return null;
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        } catch (Exception e){
            return null;
        }
    }
}
