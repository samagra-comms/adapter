package com.samagra.adapter.publisher;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

@Service
@Slf4j
public class GupshupWhatsappOutboundPub {
    private KafkaTemplate<String, String> simpleProducer;

    @Value("${gs-whatsapp-outbound-message}")
    private String WOM;

    public GupshupWhatsappOutboundPub(KafkaTemplate<String, String> simpleProducer) {
        this.simpleProducer = simpleProducer;
    }

    public void send(String message) {
        log.info("gs whatsapp outbound message response {}",simpleProducer.send(WOM, message));
    }
}
