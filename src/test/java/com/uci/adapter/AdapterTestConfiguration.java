package com.uci.adapter;

import org.mockito.Mock;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.caffeine.CaffeineCacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.web.reactive.function.client.WebClient;

import com.uci.utils.BotService;

import io.fusionauth.client.FusionAuthClient;

public class AdapterTestConfiguration {
	@Bean
	public WebClient getWebClient() {
		return WebClient.builder().baseUrl("CAMPAIGN_URL").defaultHeader("admin-token", "admin-token").build();
	}
	
	@Bean
	public FusionAuthClient getFusionAuthClient() {
		return new FusionAuthClient("fa-auth-key", "fa-auth-url");
	}

	@Autowired
	private WebClient webClient;

	@Autowired
	private FusionAuthClient fusionAuthClient;

	@Mock
	private CaffeineCacheManager cacheManager;

	@Bean
    public BotService botService() {
        return new BotService(webClient, fusionAuthClient, cacheManager);
    }
	
//	@Bean
//    public CampaignService campaignService() {
//        return new CampaignService(getWebClient(), getFusionAuthClient());
//    }
//	
//	@Bean
//    Map<String, Object> kafkaProducerConfiguration() {
//        Map<String, Object> configuration = new HashMap<>();
//        configuration.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, BOOTSTRAP_SERVERS);
//        configuration.put(ProducerConfig.CLIENT_ID_CONFIG, "sample-producer");
//        configuration.put(ProducerConfig.ACKS_CONFIG, "all");
//        configuration.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, org.springframework.kafka.support.serializer.JsonSerializer.class);
//        configuration.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, org.springframework.kafka.support.serializer.JsonSerializer.class);
//        return configuration;
//    }
//	
//	@Bean
//    public ProducerFactory<String, String> producerFactory() {
//        return new DefaultKafkaProducerFactory(kafkaProducerConfiguration());
//    }
//
//    @Bean
//    public KafkaTemplate<String, String> kafkaTemplate() {
//        return new KafkaTemplate<>(producerFactory());
//    }
//	
//	@Bean
//    SimpleProducer kafkaSimpleProducer() {
//        return new SimpleProducer(kafkaTemplate());
//    }
}
