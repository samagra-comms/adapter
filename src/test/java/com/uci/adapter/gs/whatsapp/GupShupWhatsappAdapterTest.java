package com.uci.adapter.gs.whatsapp;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.uci.adapter.AdapterTestConfiguration;
import com.uci.dao.models.XMessageDAO;
import com.uci.dao.repository.XMessageRepository;
import com.uci.utils.BotService;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import messagerosa.core.model.MediaCategory;
import messagerosa.core.model.XMessage;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import javax.xml.bind.JAXBException;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.when;

@Slf4j
@ExtendWith(MockitoExtension.class)
@SpringBootTest(classes = AdapterTestConfiguration.class)
class GupShupWhatsappAdapterTest{

    GupShupWhatsappAdapter adapter;
    ObjectMapper objectMapper;
    String simplePayload, readPayload, sentPayload, deliveredPayload, imagePayload, locationPayload, simpleXMsg, imageXMsg, quickReplyButtonXmsg, listMessageXMessage;

    @MockBean
    BotService botService;

    @MockBean
    XMessageRepository xMessageRepo;

    @MockBean
    XMessageDAO xMessageDAO;

    @SneakyThrows
    @BeforeEach
    public void init() {
//        when(botService.getCurrentAdapter(any())).thenReturn(Mono.just("A"));
//        when(botService.getCampaignFromStartingMessage(any())).thenReturn(Mono.just("test"));

        objectMapper = new ObjectMapper();

        simplePayload = "{\"waNumber\":\"919311415686\",\"mobile\":\"919415787824\",\"replyId\":null,\"messageId\":null,\"timestamp\":1616952476000,\"name\":\"chaks\",\"version\":0,\"type\":\"text\",\"text\":\"*\",\"image\":null,\"document\":null,\"voice\":null,\"audio\":null,\"video\":null,\"location\":null,\"response\":null,\"extra\":null,\"app\":null}";
        readPayload = "{\"waNumber\":null,\"mobile\":null,\"replyId\":null,\"messageId\":null,\"timestamp\":null,\"name\":null,\"version\":0,\"type\":null,\"text\":null,\"image\":null,\"document\":null,\"voice\":null,\"audio\":null,\"video\":null,\"location\":null,\"response\":\"[{\\\"srcAddr\\\":\\\"SDTEXT\\\",\\\"extra\\\":\\\"Samagra\\\",\\\"channel\\\":\\\"WHATSAPP\\\",\\\"externalId\\\":\\\"4340925846643462155-31668054994359383\\\",\\\"cause\\\":\\\"READ\\\",\\\"errorCode\\\":\\\"026\\\",\\\"destAddr\\\":\\\"919415787824\\\",\\\"eventType\\\":\\\"READ\\\",\\\"eventTs\\\":1616990315000}]\",\"extra\":null,\"app\":null}";
        sentPayload = "{\"waNumber\":null,\"mobile\":null,\"replyId\":null,\"messageId\":null,\"timestamp\":null,\"name\":null,\"version\":0,\"type\":null,\"text\":null,\"image\":null,\"document\":null,\"voice\":null,\"audio\":null,\"video\":null,\"location\":null,\"response\":\"[{\\\"srcAddr\\\":\\\"SDTEXT\\\",\\\"extra\\\":\\\"Samagra\\\",\\\"channel\\\":\\\"WHATSAPP\\\",\\\"externalId\\\":\\\"4340925846643462155-31668054994359383\\\",\\\"cause\\\":\\\"SENT\\\",\\\"errorCode\\\":\\\"025\\\",\\\"destAddr\\\":\\\"919415787824\\\",\\\"eventType\\\":\\\"SENT\\\",\\\"eventTs\\\":1616990314000}]\",\"extra\":null,\"app\":null}";
        deliveredPayload = "{\"waNumber\":null,\"mobile\":null,\"replyId\":null,\"messageId\":null,\"timestamp\":null,\"name\":null,\"version\":0,\"type\":null,\"text\":null,\"image\":null,\"document\":null,\"voice\":null,\"audio\":null,\"video\":null,\"location\":null,\"response\":\"[{\\\"srcAddr\\\":\\\"SDTEXT\\\",\\\"extra\\\":\\\"Samagra\\\",\\\"channel\\\":\\\"WHATSAPP\\\",\\\"externalId\\\":\\\"4340928795421794315-368294223055997520\\\",\\\"cause\\\":\\\"SUCCESS\\\",\\\"errorCode\\\":\\\"000\\\",\\\"destAddr\\\":\\\"919415787824\\\",\\\"eventType\\\":\\\"DELIVERED\\\",\\\"eventTs\\\":1616990666000}]\",\"extra\":null,\"app\":null}";
        imagePayload = "{\"waNumber\":\"919311415686\",\"mobile\":\"917823807161\",\"replyId\":null,\"messageId\":null,\"timestamp\":1649912157000,\"name\":\"YashwantTejwani\",\"version\":0,\"type\":\"image\",\"text\":null,\"image\":\"{\\\"signature\\\":\\\"1b3b02145958923116522815854ccb6547d79ba6d8cc872dad9cd6a75c62fd13\\\",\\\"mime_type\\\":\\\"image/jpeg\\\",\\\"url\\\":\\\"https://gs-datareceiver-whatsapp.s3.ap-south-1.amazonaws.com/4617094290724338763_52f31690-db5c-408f-965f-78c63e2d23b6?X-Amz-Algorithm=AWS4-HMAC-SHA256&X-Amz-Date=20220414T045558Z&X-Amz-SignedHeaders=host&X-Amz-Expires=172800&X-Amz-Credential=AKIAV4FTFRLFCLI4BR77%2F20220414%2Fap-south-1%2Fs3%2Faws4_request&X-Amz-Signature=\\\"}\",\"document\":null,\"voice\":null,\"audio\":null,\"video\":null,\"location\":null,\"response\":null,\"extra\":null,\"app\":null,\"interactive\":null}";
        locationPayload = "{\"waNumber\":\"919311415686\",\"mobile\":\"917823807161\",\"replyId\":null,\"messageId\":null,\"timestamp\":1649912384000,\"name\":\"YashwantTejwani\",\"version\":0,\"type\":\"location\",\"text\":null,\"image\":null,\"document\":null,\"voice\":null,\"audio\":null,\"video\":null,\"location\":\"{\\\"latitude\\\":19.1116977,\\\"longitude\\\":72.8631493}\",\"response\":null,\"extra\":null,\"app\":null,\"interactive\":null}";

        simpleXMsg = "{\"app\":\"UCIDemo\",\"messageType\":\"TEXT\",\"adapterId\":\"44a9df72-3d7a-4ece-94c5-98cf26307324\",\"messageId\":{\"channelMessageId\":null,\"replyId\":null,\"id\":null},\"to\":{\"userID\":\"7823807161\",\"groups\":null,\"campaignID\":\"UCIDemo\",\"formID\":null,\"bot\":false,\"broadcast\":false,\"meta\":null,\"deviceType\":\"PHONE\",\"deviceID\":\"91311fd1-5c1c-4f81-b9eb-2d259159554a\",\"encryptedDeviceID\":\"yOJcM+Gm7yVkKeQqPhdDKNb0wsmh8St/ty+pM5Q+4W4=\"},\"from\":{\"userID\":\"admin\",\"groups\":null,\"campaignID\":null,\"formID\":null,\"bot\":false,\"broadcast\":false,\"meta\":null,\"deviceType\":null,\"deviceID\":null,\"encryptedDeviceID\":null},\"channelURI\":\"WhatsApp\",\"providerURI\":\"gupshup\",\"timestamp\":1649913264000,\"userState\":null,\"encryptionProtocol\":null,\"messageState\":\"REPLIED\",\"lastMessageID\":\"611c1ff0-bb00-11ec-898f-6d62c3880870\",\"conversationStage\":null,\"conversationLevel\":[0,1],\"transformers\":null,\"thread\":null,\"payload\":{\"text\":\"Hi!RozgarBotwelcomesyou!\\n\\nYoumaynavigatethroughmetofirstregisteryourselfasarecruiterandthenpostvacancies.Duringtheflow,Enter#togotothepreviousstepand*togobacktotheoriginalmenu.\\n\\nPleaseselectthenumbercorrespondingtotheoptionyouwanttoproceedaheadwith.\\n\\n\",\"media\":null,\"location\":null,\"contactCard\":null,\"buttonChoices\":[{\"key\":\"1\",\"text\":\"1Registerasrecruiter\",\"backmenu\":null},{\"key\":\"2\",\"text\":\"2Postajobvacancy\",\"backmenu\":null}],\"stylingTag\":null,\"flow\":null,\"questionIndex\":null,\"mediaCaption\":null},\"provider\":\"gupshup\",\"channel\":\"WhatsApp\"}";
        imageXMsg = "{\"app\":\"UCItestlist3\",\"messageType\":\"TEXT\",\"adapterId\":\"44a9df72-3d7a-4ece-94c5-98cf26307324\",\"messageId\":{\"channelMessageId\":null,\"replyId\":null,\"id\":null},\"to\":{\"userID\":\"7823807161\",\"groups\":null,\"campaignID\":\"UCItestlist3\",\"formID\":null,\"bot\":false,\"broadcast\":false,\"meta\":null,\"deviceType\":\"PHONE\",\"deviceID\":\"91311fd1-5c1c-4f81-b9eb-2d259159554a\",\"encryptedDeviceID\":\"yOJcM+Gm7yVkKeQqPhdDKNb0wsmh8St/ty+pM5Q+4W4=\"},\"from\":{\"userID\":\"admin\",\"groups\":null,\"campaignID\":null,\"formID\":null,\"bot\":false,\"broadcast\":false,\"meta\":null,\"deviceType\":null,\"deviceID\":null,\"encryptedDeviceID\":null},\"channelURI\":\"WhatsApp\",\"providerURI\":\"gupshup\",\"timestamp\":1649913437000,\"userState\":null,\"encryptionProtocol\":null,\"messageState\":\"REPLIED\",\"lastMessageID\":\"611c1ff0-bb00-11ec-898f-6d62c3880870\",\"conversationStage\":null,\"conversationLevel\":[3,1],\"transformers\":null,\"thread\":null,\"payload\":{\"text\":\"testing-1.jpg\\n\\n\",\"media\":null,\"location\":null,\"contactCard\":null,\"buttonChoices\":null,\"stylingTag\":\"IMAGE\",\"flow\":\"employerReg\",\"questionIndex\":2,\"mediaCaption\":\"Mobile\"},\"provider\":\"gupshup\",\"channel\":\"WhatsApp\"}";
        quickReplyButtonXmsg = "{\"app\":\"UCItestlist3\",\"messageType\":\"TEXT\",\"adapterId\":\"44a9df72-3d7a-4ece-94c5-98cf26307324\",\"messageId\":{\"channelMessageId\":null,\"replyId\":null,\"id\":null},\"to\":{\"userID\":\"7823807161\",\"groups\":null,\"campaignID\":\"UCItestlist3\",\"formID\":null,\"bot\":false,\"broadcast\":false,\"meta\":null,\"deviceType\":\"PHONE\",\"deviceID\":\"91311fd1-5c1c-4f81-b9eb-2d259159554a\",\"encryptedDeviceID\":\"yOJcM+Gm7yVkKeQqPhdDKNb0wsmh8St/ty+pM5Q+4W4=\"},\"from\":{\"userID\":\"admin\",\"groups\":null,\"campaignID\":null,\"formID\":null,\"bot\":false,\"broadcast\":false,\"meta\":null,\"deviceType\":null,\"deviceID\":null,\"encryptedDeviceID\":null},\"channelURI\":\"WhatsApp\",\"providerURI\":\"gupshup\",\"timestamp\":1649913548000,\"userState\":null,\"encryptionProtocol\":null,\"messageState\":\"REPLIED\",\"lastMessageID\":\"611c1ff0-bb00-11ec-898f-6d62c3880870\",\"conversationStage\":null,\"conversationLevel\":[3,4],\"transformers\":null,\"thread\":null,\"payload\":{\"text\":\"Pleaseselectyourgender.\\n\\n\",\"media\":null,\"location\":null,\"contactCard\":null,\"buttonChoices\":[{\"key\":\"1\",\"text\":\"1Male\",\"backmenu\":null},{\"key\":\"2\",\"text\":\"2Female\",\"backmenu\":null}],\"stylingTag\":\"QUICKREPLYBTN\",\"flow\":\"employerReg\",\"questionIndex\":5,\"mediaCaption\":null},\"provider\":\"gupshup\",\"channel\":\"WhatsApp\"}";
        listMessageXMessage = "{\"app\":\"TestConstraintBot\",\"messageType\":\"TEXT\",\"adapterId\":\"44a9df72-3d7a-4ece-94c5-98cf26307324\",\"messageId\":{\"channelMessageId\":\"ABEGkXgjgHFhAgo-sCO6Xuf_dqtH\"},\"to\":{\"userID\":\"7823807161\",\"campaignID\":\"TestConstraintBot\",\"bot\":false,\"broadcast\":false,\"deviceType\":\"PHONE\",\"deviceID\":\"91311fd1-5c1c-4f81-b9eb-2d259159554a\",\"encryptedDeviceID\":\"yOJcM+Gm7yVkKeQqPhdDKNb0wsmh8St/ty+pM5Q+4W4=\"},\"from\":{\"userID\":\"admin\",\"bot\":false,\"broadcast\":false},\"channelURI\":\"WhatsApp\",\"providerURI\":\"gupshup\",\"timestamp\":1649768023000,\"messageState\":\"REPLIED\",\"lastMessageID\":\"8fbb29b0-ba5f-11ec-bb44-1df194a7fa74\",\"conversationLevel\":[5],\"payload\":{\"text\":\"Selectthirdskillfromlist?-CorrectList\\n\\n\",\"buttonChoices\":[{\"key\":\"1\",\"text\":\"1c\"},{\"key\":\"2\",\"text\":\"2photoshop\"},{\"key\":\"3\",\"text\":\"3java\"},{\"key\":\"4\",\"text\":\"4python\"},{\"key\":\"5\",\"text\":\"5html\"},{\"key\":\"6\",\"text\":\"6css\"},{\"key\":\"7\",\"text\":\"7javascript\"},{\"key\":\"8\",\"text\":\"8reactjs\"},{\"key\":\"9\",\"text\":\"9nodejs\"},{\"key\":\"10\",\"text\":\"10angularjs\"}],\"stylingTag\":\"LIST\"},\"provider\":\"gupshup\",\"channel\":\"WhatsApp\"}";

        adapter = Mockito.spy(GupShupWhatsappAdapter
                .builder()
                .botservice(botService)
                .xmsgRepo(xMessageRepo)
                .build());
    }

    @Test
    public void simplePayloadParsing() throws JsonProcessingException, JAXBException {
        ArrayList<XMessageDAO> xMessageDAOArrayList = new ArrayList<>();
        xMessageDAOArrayList.add(xMessageDAO);
//        when(xMessageRepo.findAllByUserIdOrderByTimestamp((String) notNull())).thenReturn(xMessageDAOArrayList);

        GSWhatsAppMessage message = objectMapper.readValue(simplePayload, GSWhatsAppMessage.class);
        Mono<XMessage> xMessage = adapter.convertMessageToXMsg(message);

        xMessage.log().subscribe(System.out::println,
                (e) -> System.err.println("---------------Exception occured in converting Message to XMessage-------------: " + e),
                () -> System.out.println("------------Convert Message to XMessage completed-----------"));
    }

    @Test
    public void readPayloadParsing() throws JsonProcessingException, JAXBException {

        GSWhatsAppMessage message = objectMapper.readValue(readPayload, GSWhatsAppMessage.class);
        Mono<XMessage> xMessage = adapter.convertMessageToXMsg(message);

        xMessage.log().subscribe(System.out::println,
                (e) -> System.err.println("---------------Exception occured in converting Message to XMessage-------------: " + e),
                () -> System.out.println("------------Convert Message to XMessage completed-----------"));
    }

    @Test
    public void sentPayloadParsing() throws JsonProcessingException, JAXBException {

        GSWhatsAppMessage message = objectMapper.readValue(sentPayload, GSWhatsAppMessage.class);
        Mono<XMessage> xMessage = adapter.convertMessageToXMsg(message);

        xMessage.log().subscribe(System.out::println,
                (e) -> System.err.println("---------------Exception occured in converting Message to XMessage-------------: " + e),
                () -> System.out.println("------------Convert Message to XMessage completed-----------"));
    }

    @Test
    public void deliveredPayloadParsing() throws JsonProcessingException, JAXBException {

        GSWhatsAppMessage message = objectMapper.readValue(deliveredPayload, GSWhatsAppMessage.class);
        Mono<XMessage> xMessage = adapter.convertMessageToXMsg(message);

        xMessage.log().subscribe(System.out::println,
                (e) -> System.err.println("---------------Exception occured in converting Message to XMessage-------------: " + e),
                () -> System.out.println("------------Convert Message to XMessage completed-----------"));
    }

    @Test
    public void imagePayloadParing() throws JsonProcessingException {
        GSWhatsAppMessage message = objectMapper.readValue(imagePayload, GSWhatsAppMessage.class);
        Map<String , Object> t = new HashMap<>();
        t.put("name", "abcd");
        t.put("url", "https://cdn.pixabay.com/photo/2020/06/01/22/23/eye-5248678__340.jpg");
        t.put("category", MediaCategory.IMAGE);
        t.put("size", 1000d);

        Mockito.doReturn(t).when(adapter).uploadInboundMediaFile(any(), any(), any());

        Mono<XMessage> xMessage = adapter.convertMessageToXMsg(message);


        xMessage.log().subscribe(System.out::println,
                (e) -> System.err.println("---------------Exception occured in converting Message to XMessage-------------: " + e),
                () -> System.out.println("------------Convert Message to XMessage completed-----------"));
    }

    @Test
    public  void locationPayloadParsing() throws JsonProcessingException {
        GSWhatsAppMessage message = objectMapper.readValue(locationPayload, GSWhatsAppMessage.class);
        Mono<XMessage> xMessage = adapter.convertMessageToXMsg(message);

        xMessage.log().subscribe(System.out::println,
                (e) -> System.err.println("---------------Exception occured in converting Message to XMessage-------------: " + e),
                () -> System.out.println("------------Convert Message to XMessage completed-----------"));
    }

    @Test
    public void simpleXMessageParsing() throws Exception {
        Map<String, String> credentials = new HashMap<>();
        credentials.put("usernameHSM", "username");
        credentials.put("passwordHSM", "abcd1234");
        credentials.put("username2Way", "username");
        credentials.put("password2Way", "1234");

        when(botService.getGupshupAdpaterCredentials(any())).thenReturn(Mono.just(new HashMap<>()));

        XMessage message = objectMapper.readValue(simpleXMsg, XMessage.class);
        Mono<XMessage> response = adapter.processOutBoundMessageF(message);
        StepVerifier.create(response.log()).expectNext(message).verifyComplete();
    }

    @Test
    public void imageXMessageParsing() throws Exception {
        Map<String, String> credentials = new HashMap<>();
        credentials.put("usernameHSM", "username");
        credentials.put("passwordHSM", "abcd1234");
        credentials.put("username2Way", "username");
        credentials.put("password2Way", "1234");

        when(botService.getGupshupAdpaterCredentials(any())).thenReturn(Mono.just(new HashMap<>()));

        XMessage message = objectMapper.readValue(imageXMsg, XMessage.class);
        Mono<XMessage> response = adapter.processOutBoundMessageF(message);
        StepVerifier.create(response.log()).expectNext(message).verifyComplete();
    }

    @Test
    public void quickReplyButtonXMessageParsing() throws Exception {
        Map<String, String> credentials = new HashMap<>();
        credentials.put("usernameHSM", "username");
        credentials.put("passwordHSM", "abcd1234");
        credentials.put("username2Way", "username");
        credentials.put("password2Way", "1234");

        when(botService.getGupshupAdpaterCredentials(any())).thenReturn(Mono.just(new HashMap<>()));

        XMessage message = objectMapper.readValue(quickReplyButtonXmsg, XMessage.class);
        Mono<XMessage> response = adapter.processOutBoundMessageF(message);
        StepVerifier.create(response.log()).expectNext(message).verifyComplete();
    }

    @Test
    public void ListMessageXMessageParsing() throws Exception {
        Map<String, String> credentials = new HashMap<>();
        credentials.put("usernameHSM", "username");
        credentials.put("passwordHSM", "abcd1234");
        credentials.put("username2Way", "username");
        credentials.put("password2Way", "1234");

        when(botService.getGupshupAdpaterCredentials(any())).thenReturn(Mono.just(new HashMap<>()));

        XMessage message = objectMapper.readValue(listMessageXMessage, XMessage.class);
        Mono<XMessage> response = adapter.processOutBoundMessageF(message);
        StepVerifier.create(response.log()).expectNext(message).verifyComplete();
    }


    @AfterAll
    static void teardown() {
        System.out.println("Teardown 43");
    }

}
