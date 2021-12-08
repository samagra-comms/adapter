package com.uci.adapter.gs.whatsapp;

import java.net.URI;
import java.util.Map;
import java.util.function.Consumer;
import java.util.function.Function;

import org.springframework.stereotype.Service;
import org.springframework.util.MultiValueMap;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.util.UriComponentsBuilder;

import com.uci.adapter.netcore.whatsapp.NewNetcoreService;
import com.uci.adapter.netcore.whatsapp.outbound.OutboundMessage;
import com.uci.adapter.netcore.whatsapp.outbound.SendMessageResponse;

import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import reactor.core.publisher.Mono;

public class GSWhatsappService {
	
	private final static String GUPSHUP_OUTBOUND = "https://media.smsgupshup.com/GatewayAPI/rest";

	private final WebClient webClient;
	
	private static GSWhatsappService gupshuupService = null;
	
	public GSWhatsappService() {
        webClient = WebClient.builder()
                .build();
    }
	
	public static GSWhatsappService getInstance() {
        if (gupshuupService == null) {
            return new GSWhatsappService();
        } else {
            return gupshuupService;
        }
    }
	
	public Mono<GSWhatsappOutBoundResponse> sendOutboundMessage(URI url) {
        return webClient.get()
        		.uri(url)
                .retrieve()
                .bodyToMono(GSWhatsappOutBoundResponse.class)
                .map(new Function<GSWhatsappOutBoundResponse, GSWhatsappOutBoundResponse>() {
                    @Override
                    public GSWhatsappOutBoundResponse apply(GSWhatsappOutBoundResponse sendMessageResponse) {
                        if (sendMessageResponse != null) {
                            System.out.println("RESPONSE " + sendMessageResponse.getResponse());
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
}
