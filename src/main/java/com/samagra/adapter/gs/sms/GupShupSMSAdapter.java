package com.samagra.adapter.gs.sms;

import com.samagra.adapter.provider.factory.AbstractProvider;
import com.samagra.adapter.provider.factory.IProvider;
import com.samagra.utils.GupShupUtills;
import lombok.extern.slf4j.Slf4j;
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
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import javax.xml.bind.JAXBException;
import java.nio.charset.Charset;
import java.util.*;

@Slf4j
@Service
@Qualifier("gupshupSMSAdapter")
public class GupShupSMSAdapter  extends AbstractProvider implements IProvider {

    @Value("${provider.gupshup.whatsapp.apikey}")
    private String gsApiKey;

    @Autowired
    @Qualifier("rest")
    private RestTemplate restTemplate;

    private final static String GUPSHUP_SMS_OUTBOUND = "http://enterprise.smsgupshup.com/GatewayAPI/rest";

    @Override
    public Mono<XMessage> convertMessageToXMsg(Object message) throws JAXBException{
        return null;
    }

    @Override
    public void processOutBoundMessage(XMessage xMsg) throws Exception {
        callOutBoundAPI(xMsg);
    }

    @Override
    public Mono<Boolean> processOutBoundMessageF(XMessage nextMsg) throws Exception {
        return null;
    }

    public XMessage callOutBoundAPI(XMessage xMsg) throws Exception {
        HashMap<String, String> params = new HashMap<String, String>();
        params.put("method", "SendMessage");
        params.put("send_to", xMsg.getTo().getUserID());
        params.put("msg",  xMsg.getPayload().getText());
        params.put("msg_type", "Text");
        params.put("userid", "2000164521");
        params.put("auth_scheme", "plain");
        params.put("password", "samagra_15");
        params.put("v", "1.1");
        params.put("format", "text");
//        params.put("messageTemplate","%code% is your OTP for password reset.");

        String str2 =
                URLEncodedUtils.format(GupShupUtills.hashMapToNameValuePairList(params), '&', Charset.defaultCharset());

        log.info("Question for user: {}", xMsg.getPayload().getText());

        HttpEntity<String> request = new HttpEntity<String>(str2, getVerifyHttpHeader());
        restTemplate.getMessageConverters().add(GupShupUtills.getMappingJackson2HttpMessageConverter());
        log.info(" {}",restTemplate.postForObject(GUPSHUP_SMS_OUTBOUND, request, String.class));
        return null;
    }

    private HttpHeaders getVerifyHttpHeader() throws Exception {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);
        headers.add("Cache-Control", "no-cache");
        headers.add("apikey", "8e455564878b4ca2ccb7b37f13ef9bfa");
        return headers;
    }
}
