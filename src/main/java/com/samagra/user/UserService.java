package com.samagra.user;

import com.inversoft.error.Errors;
import com.inversoft.rest.ClientResponse;
import io.fusionauth.client.FusionAuthClient;
import io.fusionauth.domain.Application;
import io.fusionauth.domain.User;
import io.fusionauth.domain.api.UserResponse;
import io.fusionauth.domain.api.user.SearchRequest;
import io.fusionauth.domain.api.user.SearchResponse;
import io.fusionauth.domain.search.UserSearchCriteria;
import lombok.extern.slf4j.Slf4j;
import messagerosa.core.model.SenderReceiverInfo;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

@Component
@Slf4j
public class UserService {

    public static User findByEmail(String email) {
        FusionAuthClient staticClient = new FusionAuthClient("c0VY85LRCYnsk64xrjdXNVFFJ3ziTJ91r08Cm0Pcjbc", "http://134.209.150.161:9011");
        ClientResponse<UserResponse, Errors> response = staticClient.retrieveUserByEmail(email);
        if (response.wasSuccessful()) {
            return response.successResponse.user;
        } else if (response.errorResponse != null) {
            Errors errors = response.errorResponse;
        } else if (response.exception != null) {
            // Exception Handling
            Exception exception = response.exception;
        }

        return null;
    }

    public static User findByPhone(String phone) {
        FusionAuthClient staticClient = new FusionAuthClient("c0VY85LRCYnsk64xrjdXNVFFJ3ziTJ91r08Cm0Pcjbc", "http://134.209.150.161:9011");
        UserSearchCriteria usc = new UserSearchCriteria();
        usc.queryString = "*" + phone + "*";
        SearchRequest sr = new SearchRequest(usc);
        ClientResponse<SearchResponse, Errors> cr = staticClient.searchUsersByQueryString(sr);

        if (cr.wasSuccessful() && cr.successResponse.users.size() > 0) {
            return cr.successResponse.users.get(0);
        } else if (cr.exception != null) {
            // Exception Handling
            Exception exception = cr.exception;
            log.error("Exception in getting users for campaign: " + exception.toString());
        }
        return null;
    }

    public static List<User> findUsersForCampaign(String campaignName) throws Exception {

        Application currentApplication = BotService.getCampaignFromName(campaignName);
        FusionAuthClient staticClient = new FusionAuthClient("c0VY85LRCYnsk64xrjdXNVFFJ3ziTJ91r08Cm0Pcjbc", "http://134.209.150.161:9011");
        if(currentApplication != null){
            UserSearchCriteria usc = new UserSearchCriteria();
            usc.queryString = "(memberships.groupId: " + currentApplication.data.get("group") + ")";
            SearchRequest sr = new SearchRequest(usc);
            ClientResponse<SearchResponse, Errors> cr = staticClient.searchUsersByQueryString(sr);

            if (cr.wasSuccessful()) {
                return cr.successResponse.users;
            } else if (cr.exception != null) {
                // Exception Handling
                Exception exception = cr.exception;
                log.error("Exception in getting users for campaign: " + exception.toString());
            }
        }
        return new ArrayList<>();
    }



    public static SenderReceiverInfo getInfoForUser(){
        return null;
    }
}
