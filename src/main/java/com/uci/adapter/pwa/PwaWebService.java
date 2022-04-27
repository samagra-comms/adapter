package com.uci.adapter.pwa;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.uci.adapter.pwa.web.outbound.PwaWebResponse;
import com.uci.adapter.pwa.web.outbound.OutboundMessage;

import okhttp3.*;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.io.IOException;
import java.util.function.Consumer;
import java.util.function.Function;

public class PwaWebService {
    private OkHttpClient client;
    private MediaType mediaType;
    private final WebClient webClient;

    private static PwaWebService webService = null;
    
    public PwaWebService(){
        this.client = new OkHttpClient().newBuilder().build();
        this.mediaType = MediaType.parse("application/json");
        this.webClient = WebClient.builder().build();
    }

    public static PwaWebService getInstance() {
        if (webService == null) {
            return new PwaWebService();
        } else {
            return webService;
        }
    }

    public PwaWebResponse sendText(String url, OutboundMessage message){
        ObjectMapper mapper = new ObjectMapper();
        RequestBody body = null;
        try {
            body = RequestBody.create(mediaType,  mapper.writeValueAsString(message));
            Request request = new Request.Builder()
                    .url(url )
                    .method("POST", body)
                    .addHeader("Content-Type", "application/json")
                    .build();
            Response response = client.newCall(request).execute();
            String json = response.body().string();
            return mapper.readValue(json, PwaWebResponse.class);
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


    public Mono<PwaWebResponse> sendOutboundMessage(String url, OutboundMessage outboundMessage) {
        return webClient.post()
                .uri(url)
                .body(Mono.just(outboundMessage), OutboundMessage.class)
                .retrieve()
                .bodyToMono(PwaWebResponse.class)
                .map(new Function<PwaWebResponse, PwaWebResponse>() {
                    @Override
                    public PwaWebResponse apply(PwaWebResponse pwaWebResponse) {
                        if (pwaWebResponse != null) {
                            System.out.println("MESSAGE RESPONSE " + pwaWebResponse.getMessage());
                            System.out.println("STATUS RESPONSE " + pwaWebResponse.getStatus());
                            System.out.println("MESSAGE ID RESPONSE " + pwaWebResponse.getId());
                            return pwaWebResponse;
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
