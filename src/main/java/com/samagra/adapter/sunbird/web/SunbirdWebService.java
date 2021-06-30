package com.samagra.adapter.sunbird.web;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.samagra.adapter.sunbird.web.outbound.OutboundMessage;
import com.samagra.adapter.sunbird.web.outbound.SunbirdWebResponse;
import okhttp3.*;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.io.IOException;
import java.util.function.Consumer;
import java.util.function.Function;

public class SunbirdWebService {
    private OkHttpClient client;
    private MediaType mediaType;
    private SunbirdCredentials credentials;
    private final WebClient webClient;

    private static SunbirdWebService sunbirdWebService = null;
    public SunbirdWebService(SunbirdCredentials credentials){
        this.client = new OkHttpClient().newBuilder().build();
        this.mediaType = MediaType.parse("application/json");
        this.credentials = credentials;
        this.webClient = WebClient.builder().build();
    }

    public static SunbirdWebService getInstance(SunbirdCredentials credentials) {
        if (sunbirdWebService == null) {
            return new SunbirdWebService(credentials);
        } else {
            return sunbirdWebService;
        }
    }

    public SunbirdWebResponse sendText(String url, OutboundMessage message){
        ObjectMapper mapper = new ObjectMapper();
        RequestBody body = null;
        try {
            body = RequestBody.create(mediaType,  mapper.writeValueAsString(message));
            Request request = new Request.Builder()
                    .url(url )
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


    public Mono<SunbirdWebResponse> sendOutboundMessage(String url, OutboundMessage outboundMessage) {
        return webClient.post()
                .uri(url)
                .body(Mono.just(outboundMessage), OutboundMessage.class)
                .retrieve()
                .bodyToMono(SunbirdWebResponse.class)
                .map(new Function<SunbirdWebResponse, SunbirdWebResponse>() {
                    @Override
                    public SunbirdWebResponse apply(SunbirdWebResponse sunbirdWebResponse) {
                        if (sunbirdWebResponse != null) {
                            System.out.println("MESSAGE RESPONSE " + sunbirdWebResponse.getMessage());
                            System.out.println("STATUS RESPONSE " + sunbirdWebResponse.getStatus());
                            System.out.println("MESSAGE ID RESPONSE " + sunbirdWebResponse.getId());
                            return sunbirdWebResponse;
                        } else {
                            return null;
                        }
                    }
                }).doOnError(new Consumer<Throwable>() {
                    @Override
                    public void accept(Throwable throwable) {
                        System.out.println("ERROR IS " + throwable.getLocalizedMessage());
                    }
                });
    }

}
