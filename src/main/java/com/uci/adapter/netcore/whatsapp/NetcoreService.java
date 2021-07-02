package com.uci.adapter.netcore.whatsapp;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.uci.adapter.netcore.whatsapp.outbound.ManageUserRequestMessage;
import com.uci.adapter.netcore.whatsapp.outbound.ManageUserResponse;
import com.uci.adapter.netcore.whatsapp.outbound.OutboundMessage;
import com.uci.adapter.netcore.whatsapp.outbound.SendMessageResponse;
import okhttp3.*;

import java.io.IOException;

public class NetcoreService {

    private OkHttpClient client;
    private MediaType mediaType;
    private String baseURL;
    private NWCredentials credentials;


    public NetcoreService(NWCredentials credentials){
        this.client = new OkHttpClient().newBuilder().build();
        this.mediaType = MediaType.parse("application/json");
        this.baseURL = "https://waapi.pepipost.com/api/v2/";
        this.credentials = credentials;
    }

    public ManageUserResponse manageUser(ManageUserRequestMessage message){
        ObjectMapper mapper = new ObjectMapper();
        RequestBody body = null;
        try {
            body = RequestBody.create(mediaType,  mapper.writeValueAsString(message));
            Request request = new Request.Builder()
                    .url(baseURL + "consent/manage")
                    .method("POST", body)
                    .addHeader("Content-Type", "application/json")
                    .addHeader("Authorization", "Bearer " + credentials.getToken())
                    .build();
            Response response = client.newCall(request).execute();
            String json = response.body().toString();
            return mapper.readValue(json, ManageUserResponse.class);
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

    public SendMessageResponse sendText(OutboundMessage message){
        ObjectMapper mapper = new ObjectMapper();
        RequestBody body = null;
        try {
            body = RequestBody.create(mediaType,  mapper.writeValueAsString(message));
            Request request = new Request.Builder()
                    .url(baseURL + "message/")
                    .method("POST", body)
                    .addHeader("Content-Type", "application/json")
                    .addHeader("Authorization", "Bearer " + credentials.getToken())
                    .build();
            Response response = client.newCall(request).execute();
            String json = response.body().string();
            return mapper.readValue(json, SendMessageResponse.class);
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
