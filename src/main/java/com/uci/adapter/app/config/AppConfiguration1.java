package com.uci.adapter.app.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.uci.utils.BotService;
import com.uci.utils.CampaignService;
import com.uci.utils.service.VaultService;
import io.fusionauth.client.FusionAuthClient;

import java.time.Duration;

import org.apache.hc.client5.http.auth.CredentialsProvider;
import org.apache.hc.client5.http.impl.auth.BasicCredentialsProvider;
import org.apache.hc.client5.http.auth.AuthScope;
import org.apache.hc.client5.http.auth.Credentials;
import org.apache.hc.client5.http.auth.UsernamePasswordCredentials;
import org.apache.hc.client5.http.classic.HttpClient;
import org.apache.hc.client5.http.impl.classic.HttpClients;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.cache.CacheManager;
import org.springframework.cache.caffeine.CaffeineCacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.HttpComponentsClientHttpRequestFactory;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.reactive.function.client.WebClient;

@Configuration
@EnableAutoConfiguration
public class AppConfiguration1 {

    @Value("${campaign.url}")
    public String CAMPAIGN_URL;


    @Bean
    @Qualifier("rest")
    public RestTemplate getRestTemplate() {
        return new RestTemplate();
    }

    @Bean
    @Qualifier("custom")
    public RestTemplate getCustomTemplate(RestTemplateBuilder builder) {
        Credentials credentials = new UsernamePasswordCredentials("test","abcd1234".toCharArray());
        CredentialsProvider credentialsProvider = new BasicCredentialsProvider();
        ((BasicCredentialsProvider) credentialsProvider).setCredentials(new AuthScope(null, -1), credentials);

        HttpClient httpClient = HttpClients
                .custom()
                .setDefaultCredentialsProvider(credentialsProvider)
                .disableAuthCaching()
                .build();

        return builder
                .requestFactory(() -> new HttpComponentsClientHttpRequestFactory(httpClient))
                .build();
    }

    @Bean
    @Qualifier("json")
    public RestTemplate getJSONRestTemplate() {
        return new RestTemplateBuilder()
                .additionalMessageConverters(new MappingJackson2HttpMessageConverter(new ObjectMapper()))
                .build();
    }

    @Value("${fusionauth.url}")
    public String FUSIONAUTH_URL;

    @Value("${fusionauth.key}")
    public String FUSIONAUTH_KEY;
    
    @Autowired
    public Cache<Object, Object> cache;

    @Bean
    public FusionAuthClient getFAClient() {
        return new FusionAuthClient(FUSIONAUTH_KEY, FUSIONAUTH_URL);
    }

    @Bean
    public BotService getBotService() {
    	WebClient webClient = WebClient.builder()
                .baseUrl(CAMPAIGN_URL)
                .build();
        
    	return new BotService(webClient, getFAClient(), cache);
    }

    @Bean
    public VaultService getVaultService() {
        return new VaultService(cache);
    }
}
