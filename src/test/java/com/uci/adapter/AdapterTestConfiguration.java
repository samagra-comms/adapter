package com.uci.adapter;

import com.github.benmanes.caffeine.cache.Cache;
import com.uci.adapter.netcore.whatsapp.NewNetcoreService;
import okhttp3.OkHttpClient;
import org.mockito.Mock;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.web.reactive.function.client.WebClient;

import com.uci.utils.BotService;

import io.fusionauth.client.FusionAuthClient;

public class AdapterTestConfiguration {

	@Autowired
	private WebClient webClient;

	@Autowired
	private FusionAuthClient fusionAuthClient;

	@Mock
	private Cache<Object, Object> cache;

	@Bean
	public WebClient getWebClient() {
		return WebClient.builder().baseUrl("CAMPAIGN_URL").defaultHeader("admin-token", "admin-token").build();
	}
	
	@Bean
	public FusionAuthClient getFusionAuthClient() {
		return new FusionAuthClient("fa-auth-key", "fa-auth-url");
	}

	@Bean
    public BotService botService() {
        return new BotService(webClient, fusionAuthClient, cache);
    }

	@Bean
	public NewNetcoreService newNetcoreService() {
		return new NewNetcoreService();
	}

	@Bean
	public OkHttpClient okHttpClient() {
		return new OkHttpClient().newBuilder().build();
	}
}
