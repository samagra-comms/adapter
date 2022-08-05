package com.uci.adapter.gs.sms;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.uci.adapter.gs.sms.outbound.GSSMSResponse;
import com.uci.adapter.gs.sms.outbound.GupshupSMSResponse;
import com.uci.adapter.gs.whatsapp.GSWhatsappOutBoundResponse;
import com.uci.adapter.gs.whatsapp.GSWhatsappService;
import com.uci.adapter.provider.factory.AbstractProvider;
import com.uci.adapter.provider.factory.IProvider;
import com.uci.adapter.utils.GupShupUtills;

import com.uci.utils.BotService;
import lombok.Builder;
import lombok.extern.slf4j.Slf4j;
import messagerosa.core.model.MessageId;
import messagerosa.core.model.XMessage;
import org.apache.http.client.utils.URLEncodedUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;
import reactor.core.publisher.Mono;

import javax.xml.bind.JAXBException;
import java.net.URI;
import java.nio.charset.Charset;
import java.util.*;
import java.util.function.Consumer;
import java.util.function.Function;

@Slf4j
@Service
@Qualifier("gupshupSMSAdapter")
public class GupShupSMSAdapter  extends AbstractProvider implements IProvider {

    @Autowired
    @Qualifier("rest")
    private RestTemplate restTemplate;

    @Autowired
    private BotService botService;

    private final static String GUPSHUP_SMS_OUTBOUND = "http://enterprise.smsgupshup.com/GatewayAPI/rest";

    /**
     * Convert Gupshsup Message Format to XMessage Format for inbound
     * @param message
     * @return
     * @throws JAXBException
     */
    @Override
    public Mono<XMessage> convertMessageToXMsg(Object message) throws JAXBException{
        return null;
    }

    /* Not in use */
//    @Override
//    public void processOutBoundMessage(XMessage xMsg) throws Exception {
//        callOutBoundAPI(xMsg);
//    }

    /**
     * Process outbound message - send outbound message & Mono xmsg
     * @param xMsg
     * @return
     * @throws Exception
     */
    @Override
//    public Mono<XMessage> processOutBoundMessageF(XMessage xMsg) throws Exception {
//        return Mono.just(callOutBoundAPI(xMsg));
//    }

    /**
     * Call gupshup sms outbound sms to send sms message to user & return xmsg
     * @param xMsg
     * @return
     * @throws Exception
     */
    public Mono<XMessage> processOutBoundMessageF(XMessage xMsg) throws Exception {
        String adapterIdFromXML = xMsg.getAdapterId();

        return botService.getAdapterCredentials(adapterIdFromXML)
                .map(new Function<JsonNode, Mono<XMessage>>() {
                    @Override
                        public Mono<XMessage> apply(JsonNode credentials) {
                            if (credentials != null && !credentials.isEmpty()) {
                                UriComponentsBuilder builder = UriComponentsBuilder.fromHttpUrl(GUPSHUP_SMS_OUTBOUND);

                                builder.queryParam("method", "SendMessage");
                                builder.queryParam("send_to", xMsg.getTo().getUserID());
                                builder.queryParam("msg",  xMsg.getPayload().getText());
                                builder.queryParam("msg_type", "Text");
                                builder.queryParam("messageId", "123456781");
                                builder.queryParam("userid", credentials.findValue("username").asText());
                                builder.queryParam("auth_scheme", "plain");
                                builder.queryParam("password", credentials.findValue("password").asText());
                                builder.queryParam("v", "1.1");
                                builder.queryParam("format", "json");
                                builder.queryParam("data_encoding", "text");
                                builder.queryParam("extra", "Samagra");

                                URI expanded = URI.create(builder.toUriString());
                                log.info(expanded.toString());

                                return GSSMSService.getInstance().sendOutboundMessage(expanded).map(new Function<GupshupSMSResponse, XMessage>() {
                                    @Override
                                    public XMessage apply(GupshupSMSResponse response) {
                                        if(response != null){
                                            xMsg.setMessageId(MessageId.builder().channelMessageId(response.getResponse().getId()).build());
                                            xMsg.setMessageState(XMessage.MessageState.SENT);
                                            return xMsg;
                                        }
                                        return xMsg;
                                    }
                                }).doOnError(new Consumer<Throwable>() {
                                    @Override
                                    public void accept(Throwable throwable) {
                                        log.error("Error in Send GS Whatsapp Outbound Message" + throwable.getMessage());
                                    }
                                });
//                                RestTemplate restTemplate = new RestTemplate();
//                                GupshupSMSResponse response = restTemplate.getForObject(expanded, GupshupSMSResponse.class);
//                                try {
//                                    log.info("response ================{}", new ObjectMapper().writeValueAsString(response));
//                                } catch (JsonProcessingException e) {
//                                    // TODO Auto-generated catch block
//                                    e.printStackTrace();
//                                    log.error("Error in callOutBoundAPI for objectmapper: "+e.getMessage());
//                                }
//                                xMsg.setMessageId(MessageId.builder().channelMessageId(response.getResponse().getId()).build());
//                                xMsg.setMessageState(XMessage.MessageState.SENT);
//
//                                return xMsg;
                            } else {
                                log.error("Credentials not found");
                                //                      xMsg.setMessageId(MessageId.builder().channelMessageId("").build());
                                xMsg.setMessageState(XMessage.MessageState.NOT_SENT);
                                return Mono.just(xMsg);
                            }
                        }
                }).flatMap(new Function<Mono<XMessage>, Mono<? extends XMessage>>() {
                    @Override
                    public Mono<? extends XMessage> apply(Mono<XMessage> o) {
                        return o;
                    }
                });

    }
}
