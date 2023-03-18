package com.uci.adapter.gs.sms;

import com.uci.adapter.gs.sms.outbound.GupshupSMSResponse;
import com.uci.adapter.gs.whatsapp.GSWhatsappOutBoundResponse;
import com.uci.adapter.gs.whatsapp.GSWhatsappService;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.net.URI;
import java.util.function.Consumer;
import java.util.function.Function;

public class GSSMSService {
    private final static String GUPSHUP_OUTBOUND = "https://media.smsgupshup.com/GatewayAPI/rest";

    private final WebClient webClient;

    private static GSSMSService gupshupService = null;

    public GSSMSService() {
        webClient = WebClient.builder()
                .build();
    }

    public static GSSMSService getInstance() {
        if (gupshupService == null) {
            return new GSSMSService();
        } else {
            return gupshupService;
        }
    }

    public Mono<GupshupSMSResponse> sendOutboundMessage(URI url) {
        return webClient.get()
                .uri(url)
                .retrieve()
                .bodyToMono(GupshupSMSResponse.class)
                .map(new Function<GupshupSMSResponse, GupshupSMSResponse>() {
                    @Override
                    public GupshupSMSResponse apply(GupshupSMSResponse sendMessageResponse) {
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
