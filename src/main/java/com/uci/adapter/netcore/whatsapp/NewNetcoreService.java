package com.uci.adapter.netcore.whatsapp;

import com.azure.core.util.BinaryData;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectWriter;
import com.uci.adapter.netcore.whatsapp.outbound.ManageUserRequestMessage;
import com.uci.adapter.netcore.whatsapp.outbound.ManageUserResponse;
import com.uci.adapter.netcore.whatsapp.outbound.OutboundMessage;
import com.uci.adapter.netcore.whatsapp.outbound.OutboundOptInOutMessage;
import com.uci.adapter.netcore.whatsapp.outbound.SendMessageResponse;
import okhttp3.*;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.io.IOException;
import java.io.InputStream;
import java.util.Objects;
import java.util.function.Consumer;
import java.util.function.Function;

@Service
public class NewNetcoreService {

    private final WebClient webClient;

	@Autowired
    private OkHttpClient client;
    private MediaType mediaType;
    private String baseURL;
    private NWCredentials credentials;

    private static NewNetcoreService newNetcoreService = null;

    public NewNetcoreService() {
        this.mediaType = MediaType.parse("application/json");
        String url = System.getenv("NETCORE_WHATSAPP_URI");
        url = url != null && !url.isEmpty() ? url : "https://waapi.pepipost.com/api/v2/";
        this.baseURL = url;
        this.credentials = new NWCredentials(System.getenv("NETCORE_WHATSAPP_AUTH_TOKEN"));
        webClient = WebClient.builder()
                .baseUrl(url)
                .defaultHeader("Content-Type", "application/json")
                .defaultHeader("Authorization", "Bearer " + credentials.getToken())
                .build();
    }

    public static NewNetcoreService getInstance() {
		return Objects.requireNonNullElseGet(newNetcoreService, NewNetcoreService::new);
    }

    public ManageUserResponse manageUser(ManageUserRequestMessage message) {
        ObjectMapper mapper = new ObjectMapper();
        RequestBody body = null;
        try {
            body = RequestBody.create(mediaType, mapper.writeValueAsString(message));
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
        } catch (Exception e) {
            return null;
        }
    }

    public SendMessageResponse sendText(OutboundMessage message) {
        ObjectMapper mapper = new ObjectMapper();
        RequestBody body = null;
        try {
            body = RequestBody.create(mediaType, mapper.writeValueAsString(message));
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
        } catch (Exception e) {
            return null;
        }
    }

    public Mono<SendMessageResponse> sendOutboundMessage(OutboundMessage outboundMessage) {
    	ObjectWriter ow = new ObjectMapper().writer().withDefaultPrettyPrinter();
    	try {
			String json = ow.writeValueAsString(outboundMessage);
			System.out.println("json:"+json);
		} catch (JsonProcessingException e) {
			System.out.println("json not converted:"+e.getMessage());
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
    	
        return webClient.post()
                .uri("/message/")
                .body(Mono.just(outboundMessage), OutboundMessage.class)
                .retrieve()
                .bodyToMono(SendMessageResponse.class)
                .map(new Function<SendMessageResponse, SendMessageResponse>() {
                    @Override
                    public SendMessageResponse apply(SendMessageResponse sendMessageResponse) {
                        if (sendMessageResponse != null) {
                            System.out.println("MESSAGE RESPONSE " + sendMessageResponse.getMessage());
                            System.out.println("STATUS RESPONSE " + sendMessageResponse.getStatus());
                            System.out.println("DATA RESPONSE " + sendMessageResponse.getData());
                            return sendMessageResponse;
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
    
    public Mono<SendMessageResponse> sendOutboundOptInOutMessage(OutboundOptInOutMessage outboundMessage) {
    	ObjectWriter ow = new ObjectMapper().writer().withDefaultPrettyPrinter();
    	try {
			String json = ow.writeValueAsString(outboundMessage);
			System.out.println("json:"+json);
		} catch (JsonProcessingException e) {
			System.out.println("json not converted:"+e.getMessage());
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
    	
        return webClient.post()
                .uri("/consent/manage")
                .body(Mono.just(outboundMessage), OutboundOptInOutMessage.class)
                .retrieve()
                .bodyToMono(SendMessageResponse.class)
                .map(new Function<SendMessageResponse, SendMessageResponse>() {
                    @Override
                    public SendMessageResponse apply(SendMessageResponse sendMessageResponse) {
                        if (sendMessageResponse != null) {
                            System.out.println("sendOutboundOptInOutMessage MESSAGE RESPONSE " + sendMessageResponse.getMessage());
                            System.out.println("sendOutboundOptInOutMessage STATUS RESPONSE " + sendMessageResponse.getStatus());
                            System.out.println("sendOutboundOptInOutMessage DATA RESPONSE " + sendMessageResponse.getData());
                            return sendMessageResponse;
                        } else {
                        	System.out.println("sendOutboundOptInOutMessage response is null.");
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
    
    /**
     * Get Media File from netcore by id
     * @param id
     * @return
     */
    public InputStream getMediaFile(String id) {
    	ObjectMapper mapper = new ObjectMapper();
        try {
            Request request = new Request.Builder()
                    .url(baseURL + "media/"+id)
                    .get()
                    .addHeader("Authorization", "Bearer " + credentials.getToken())
                    .build();
            
            Response response = client.newCall(request).execute();

            ResponseBody body = response.body();
            
            InputStream in = body.byteStream();
            
            if(body.contentLength() <= 0) {
            	System.out.println("Media file content length is 0");
            	return null;
            }
            
            return in;
        } catch (Exception e ) {
        	System.out.println("Exception in netcore getMediaFile: "+e.getMessage());
            e.printStackTrace();
            return null;
        }
    }
}
