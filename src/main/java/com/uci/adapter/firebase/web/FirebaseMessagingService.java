package com.uci.adapter.firebase.web;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import lombok.extern.slf4j.Slf4j;
import okhttp3.*;
import org.apache.http.HttpHeaders;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.io.IOException;

@Slf4j
@Service
public class FirebaseMessagingService {

    public static final String url = "https://fcm.googleapis.com/fcm/send";

    private String getServerKey() {
        String key = System.getenv("FIREBASE_SERVER_KEY");
        return key != null && !key.isEmpty() ? key : "";
    }

    /**
     * Send FCM Notification to token with title & body
     * @param token
     * @param title
     * @param body
     * @return
     */
    public Mono<Boolean> sendNotificationMessage(String serviceKey, String token, String title, String body) {
        WebClient client = WebClient.builder()
                .baseUrl(url)
                .defaultHeaders(httpHeaders -> {
                    httpHeaders.set("Authorization", "key="+serviceKey);
                    httpHeaders.set("Content-Type", "application/json");
                })
                .build();

        ObjectMapper mapper = new ObjectMapper();
        ObjectNode node = mapper.createObjectNode();
        node.put("to", token);
        node.put("collapse_key", "type_a");
        ObjectNode notificationNode = mapper.createObjectNode();
        notificationNode.put("body", body);
        notificationNode.put("title", title);
        node.put("notification", notificationNode);

        ObjectNode dataNode = mapper.createObjectNode();
        dataNode.put("body", body);
        dataNode.put("title", title);
        node.put("data", dataNode);

        return client.post().bodyValue(node.toString()).retrieve().bodyToMono(String.class).map(response -> {
            if (response != null) {
                try {
                    ObjectNode resultNode = (ObjectNode) mapper.readTree(response);
                    if (resultNode.get("success") != null && Integer.parseInt(resultNode.get("success").toString()) >= 1) {
                        return true;
                    }
                } catch (JsonProcessingException jsonMappingException) {
                    log.error("Exception in sendNotificationMessage: "+jsonMappingException.getMessage());
                } catch (NumberFormatException ex) {
                    log.error("Exception in sendNotificationMessage: "+ex.getMessage());
                }
            }
            return false;
        });
    }

//    public Mono<Boolean> sendNotificationMessage2(String token, String title, String body) {
//        OkHttpClient client = new OkHttpClient().newBuilder().build();
//        ObjectMapper mapper = new ObjectMapper();
//
//        ObjectNode node = mapper.createObjectNode();
//        node.put("to", token);
//        node.put("collapse_key", "type_a");
//        ObjectNode notificationNode = mapper.createObjectNode();
//        notificationNode.put("body", body);
//        notificationNode.put("title", title);
//        node.put("notification", notificationNode);
//
//        ObjectNode dataNode = mapper.createObjectNode();
//        dataNode.put("body", body);
//        dataNode.put("title", title);
//        node.put("data", dataNode);
//
//        RequestBody requestBody = null;
//        try {
//            requestBody = RequestBody.create(MediaType.parse("application/json"),  mapper.writeValueAsString(node));
//            Request request = new Request.Builder()
//                    .url(url)
//                    .method("POST", requestBody)
//                    .addHeader("Content-Type", "application/json")
//                    .addHeader("Authorization", "key=AAAAZiL4qhQ:APA91bHfQkDIXbBbChCA3AUo5Wx9eRrAE2RWjtkLBCMxOJGmQGrUXqezKBy54xGJDegR6dM8H39r6XkSVDUOQUZ0QO9-Q_vexAM9UDCvCzZnerh8k1dIFUdIaQKdP8cRCW5KJG3TB167")
//                    .build();
//            Response response = client.newCall(request).execute();
//            String json = response.body().string();
//
//            ObjectNode resultNode = (ObjectNode) mapper.readTree(json);
//            if(resultNode.get("success") != null && Integer.parseInt(resultNode.get("success").toString()) > 1) {
//                return Mono.just(true);
//            }
//        } catch (JsonProcessingException e) {
//            log.error("JsonProcessingException in send firebase notification: "+e.getMessage());
//        } catch (IOException e) {
//            log.error("IOException in send firebase notification: "+e.getMessage());
//        } catch (Exception e){
//            log.error("Exception in send firebase notification: "+e.getMessage());
//        }
//        return Mono.just(false);
//    }
}
