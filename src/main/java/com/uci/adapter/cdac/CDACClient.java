package com.uci.adapter.cdac;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import messagerosa.core.model.XMessage;
import org.springframework.scheduling.annotation.Async;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import jakarta.xml.bind.JAXBContext;
import jakarta.xml.bind.Unmarshaller;
import java.io.ByteArrayInputStream;
import java.net.URI;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;

@AllArgsConstructor
@Builder
@NoArgsConstructor
@Slf4j
public class CDACClient {
    int batchSize;
    String username;
    String password;
    XMessage xMsg;
    String message;
    String baseURL;
    String trackBaseURL;

    public List<String> sendBulkSMS() throws Exception {
        String allIDs = xMsg.getTo().getUserID();
        List<String> ids = Arrays.asList(allIDs.split(","));
        int numOfBatches = (int) Math.ceil((double) ids.size() / batchSize);
        List<CompletableFuture<String>> futures = new ArrayList<>();
        int start, end;
        for(int i=0; i<numOfBatches; i++){
            start = i*batchSize;
            end = (i+1)*batchSize;
            if (start > ids.size()) break;
            if (end > ids.size()) end = ids.size();

            futures.add(sendSingleBatch(getURL(getIDs(start, end))));
        }

        List<String> messageIds = new ArrayList<>();
        for(CompletableFuture<String> future: futures){
            messageIds.add(future.get());
        }
        log.info("All messages sent");
        return messageIds;
    }

    private String getIDs(int start, int end){
        String[] userIDs = xMsg.getTo().getUserID().split(",");
        String[] strippedIDs = Arrays.copyOfRange(userIDs, start, end);
        StringBuilder sb = new StringBuilder();
        String prefix = "";
        for (String serverId : strippedIDs) {
            sb.append(prefix);
            prefix = ",";
            sb.append(serverId);
        }
        return sb.toString();
    }

    public URI getURL(String userIDs){
        UriComponentsBuilder builder = UriComponentsBuilder.fromHttpUrl(baseURL)
                .queryParam("password", password)
                .queryParam("username", username)
                .queryParam("bulkmobno", userIDs)
                .queryParam("content", message)
                .queryParam("smsservicetype", "unicodemsg")
                .queryParam("senderid", xMsg.getFrom().getMeta().get("senderID"));

        URI expanded = URI.create(builder.toUriString());
        return expanded;
    }

    @Async
    public CompletableFuture<String> sendSingleBatch(URI uri){
        RestTemplate restTemplate = new RestTemplate();
        String response = restTemplate.postForObject(uri, null, String.class);
        String messageID = getChannelMessageId(response);
        if (messageID.equals("")) {
            return CompletableFuture.completedFuture("-1");
        } else {
            return CompletableFuture.completedFuture(messageID);
        }
    }


    private static String getChannelMessageId(String response) {
        try {
            String messageID = response.split(" = ")[1].trim();
            if (messageID.matches("[0-9]+hpgovt-hpssa")) return response.split(" = ")[1].trim();
            else return "";
        } catch (Exception e) {
            return "";
        }
    }

    public TrackDetails trackMultipleMessages(String messageIDsString) throws ExecutionException, InterruptedException {
        String[] messageIDs = messageIDsString.split(",");
        List<CompletableFuture<TrackDetails>> futures = new ArrayList<>();
        for(String messageID: messageIDs){
            futures.add(trackMessage(messageID));
        }

        List<TrackDetails> trackDetails = new ArrayList<>();
        for(CompletableFuture<TrackDetails> future: futures){
            trackDetails.add(future.get());
        }
        return mergeTrackDetails(trackDetails);

    }

    public TrackDetails mergeTrackDetails(List<TrackDetails> trackDetails) {
        TrackDetails t = new TrackDetails();

        List<PhoneNumberStatus> delivered = new ArrayList<>();
        List<PhoneNumberStatus> submitted = new ArrayList<>();
        List<PhoneNumberStatus> failed = new ArrayList<>();

        int failedSMSCount = 0;
        int submittedSMSCount = 0;
        int deliveredSMSCount = 0;
        String undelivered = "";

        StringBuilder sb = new StringBuilder();
        String prefix = "";
        for(TrackDetails tt: trackDetails){
            failedSMSCount += Integer.parseInt(tt.getFailedSMSCount());
            submittedSMSCount += Integer.parseInt(tt.getSubmittedSMSCount());
            deliveredSMSCount += Integer.parseInt(tt.getDeliveredSMSCount());
            undelivered += tt.getUndelivered();

            if(tt.getDelivered() != null && tt.getDelivered().getPhoneNumberStatus() != null) delivered.addAll(tt.getDelivered().getPhoneNumberStatus());
            if(tt.getSubmitted() != null && tt.getSubmitted().getPhoneNumberStatus() != null) submitted.addAll(tt.getSubmitted().getPhoneNumberStatus());
            if(tt.getFailed() != null && tt.getFailed().getPhoneNumberStatus() != null) failed.addAll(tt.getFailed().getPhoneNumberStatus());

            sb.append(prefix);
            prefix = ",";
            sb.append(tt.getMessageId());
        }
        t.setDeliveredSMSCount(String.valueOf(deliveredSMSCount));
        t.setSubmittedSMSCount(String.valueOf(submittedSMSCount));
        t.setFailedSMSCount(String.valueOf(failedSMSCount));
        t.setUndelivered(undelivered);

        DeliveryDetails del = new DeliveryDetails();
        del.setPhoneNumberStatus(delivered);

        DeliveryDetails sub = new DeliveryDetails();
        sub.setPhoneNumberStatus(submitted);

        DeliveryDetails fail = new DeliveryDetails();
        fail.setPhoneNumberStatus(failed);

        t.setDelivered(del);
        t.setFailed(fail);
        t.setSubmitted(sub);
        t.setMessageId(sb.toString());

        return t;

    }

    @Async
    public CompletableFuture<TrackDetails> trackMessage(String messageID){
        log.info("Called");
        UriComponentsBuilder builder = UriComponentsBuilder.fromHttpUrl(trackBaseURL)
                .queryParam("password", password)
                .queryParam("userid", username)
                .queryParam("msgid", messageID);

        URI expanded = URI.create(builder.toUriString());
        RestTemplate restTemplate = new RestTemplate();
        String response = restTemplate.getForObject(expanded, String.class);
        JAXBContext context;
        TrackDetails trackSMSResponse = null;
        try{
            context = JAXBContext.newInstance(TrackDetails.class);
            Unmarshaller jaxbUnmarshaller = context.createUnmarshaller();
            trackSMSResponse = (TrackDetails) jaxbUnmarshaller.unmarshal((new ByteArrayInputStream(response.getBytes())));
        }catch (Exception e){
            System.out.println("Error in tracking");
        }
        return CompletableFuture.completedFuture(trackSMSResponse);
    }
}
