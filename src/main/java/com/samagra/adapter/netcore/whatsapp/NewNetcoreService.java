package com.samagra.adapter.netcore.whatsapp;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.samagra.adapter.netcore.whatsapp.outbound.ManageUserRequestMessage;
import com.samagra.adapter.netcore.whatsapp.outbound.ManageUserResponse;
import com.samagra.adapter.netcore.whatsapp.outbound.OutboundMessage;
import com.samagra.adapter.netcore.whatsapp.outbound.SendMessageResponse;
import okhttp3.*;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.io.IOException;
import java.util.function.Consumer;

public class NewNetcoreService {

    private final WebClient webClient;
    private OkHttpClient client;
    private MediaType mediaType;
    private String baseURL;
    private NWCredentials credentials;

    private static NewNetcoreService newNetcoreService = null;

    public NewNetcoreService(NWCredentials credentials) {
        this.client = new OkHttpClient().newBuilder().build();
        this.mediaType = MediaType.parse("application/json");
        this.baseURL = "https://waapi.pepipost.com/api/v2/";
        this.credentials = credentials;
        webClient = WebClient.builder()
                .baseUrl("https://waapi.pepipost.com/api/v2")
                .defaultHeader("Content-Type", "application/json")
                .defaultHeader("Authorization", "Bearer " + credentials.getToken())
                .build();
    }

    public static NewNetcoreService getInstance(NWCredentials credentials){
       if(newNetcoreService == null){
           return new NewNetcoreService(credentials);
       }
       else{
           return newNetcoreService;
       }
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

    public Mono<Boolean> sendOutboundMessage(OutboundMessage outboundMessage) {
        return webClient.post()
                .uri("/message/")
                .body(Mono.just(outboundMessage), OutboundMessage.class)
                .retrieve()
                .bodyToMono(SendMessageResponse.class)
                .map(sendMessageResponse -> {
                    if(sendMessageResponse != null){
                        System.out.println("MESSAGE RESPONSE "+ sendMessageResponse.getMessage());
                        System.out.println("STATUS RESPONSE "+ sendMessageResponse.getStatus());
                        System.out.println("DATA RESPONSE "+ sendMessageResponse.getData());
//                        xMsg.setMessageId(MessageId.builder().channelMessageId(sendMessageResponse.getData().getIdentifier()).build());
//                        xMsg.setMessageState(XMessage.MessageState.SENT);
//
//                        XMessageDAO dao = XMessageDAOUtills.convertXMessageToDAO(xMsg);
//                        xmsgRepo.save(dao);
                        return true;
                    }else{
                        return false;
                    }
                }).doOnError(new Consumer<Throwable>() {
                    @Override
                    public void accept(Throwable throwable) {
                        System.out.println("ERROR IS " + throwable.getLocalizedMessage());
                    }
                });
    }
}
