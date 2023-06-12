package com.uci.adapter.app.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.benmanes.caffeine.cache.Cache;
import com.google.auth.oauth2.GoogleCredentials;
import com.google.firebase.FirebaseApp;
import com.google.firebase.FirebaseOptions;
import com.google.firebase.messaging.FirebaseMessaging;
import com.uci.utils.BotService;
import io.fusionauth.client.FusionAuthClient;

import okhttp3.OkHttpClient;
import org.apache.http.auth.AuthScope;
import org.apache.http.auth.Credentials;
import org.apache.http.auth.UsernamePasswordCredentials;
import org.apache.http.client.CredentialsProvider;
import org.apache.http.client.HttpClient;
import org.apache.http.impl.client.BasicCredentialsProvider;
import org.apache.http.impl.client.HttpClients;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.HttpComponentsClientHttpRequestFactory;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.reactive.function.client.WebClient;

import java.io.FileInputStream;
import java.io.IOException;

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
        Credentials credentials = new UsernamePasswordCredentials("test", "abcd1234");
        CredentialsProvider credentialsProvider = new BasicCredentialsProvider();
        credentialsProvider.setCredentials(AuthScope.ANY, credentials);

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
    public OkHttpClient getOkHttpClient() {
        return new OkHttpClient().newBuilder().build();
    }

}
