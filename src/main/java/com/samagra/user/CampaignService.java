package com.samagra.user;

import com.inversoft.error.Errors;
import com.inversoft.rest.ClientResponse;
import io.fusionauth.client.FusionAuthClient;
import io.fusionauth.domain.Application;
import io.fusionauth.domain.Group;
import io.fusionauth.domain.GroupMember;
import io.fusionauth.domain.User;
import io.fusionauth.domain.api.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.*;

@Component
@Slf4j
public class CampaignService {

    /**
     * Retrieve Campaign Params From its Identifier
     *
     * @param campaignID - Campaign Identifier
     * @return Application
     * @throws Exception Error Exception, in failure in Network request.
     */
    public static Application getCampaignFromID(String campaignID) throws Exception {
        System.out.println("CampaignID: " + campaignID);
        FusionAuthClient staticClient = new FusionAuthClient("c0VY85LRCYnsk64xrjdXNVFFJ3ziTJ91r08Cm0Pcjbc", "http://134.209.150.161:9011");
        System.out.println("Client: " + staticClient);
        ClientResponse<ApplicationResponse, Void> applicationResponse = staticClient.retrieveApplication(UUID.fromString(campaignID));
        if (applicationResponse.wasSuccessful()) {
            Application application = applicationResponse.successResponse.application;
            return application;
        } else if (applicationResponse.exception != null) {
            Exception exception = applicationResponse.exception;
            throw exception;
        }
        return null;
    }

    /**
     * Retrieve Campaign Params From its Name
     *
     * @param campaignName - Campaign Name
     * @return Application
     * @throws Exception Error Exception, in failure in Network request.
     */
    public static Application getCampaignFromName(String campaignName) throws Exception {
        List<Application> applications = getApplications();

        Application currentApplication = null;
        if (applications.size() > 0) {
            for (Application application : applications) {
                if (application.name.equals(campaignName)) {
                    currentApplication = application;
                }
            }
        }
        return currentApplication;
    }

    /**
     * Retrieve Campaign Params From its Name
     *
     * @param appName - Gupshup bot AppName
     * @return Application
     * @throws Exception Error Exception, in failure in Network request.
     */
    public static Application getCampaignFromGupshupAppName(String appName) {
        List<Application> applications = getApplications();

        Application currentApplication = null;
        if (applications.size() > 0) {
            for (Application application : applications) {
                try {
                    if (application.data.get("appName").equals(appName)) {
                        currentApplication = application;
                    }
                } catch (Exception e) {
                    log.info("Campaign has not app Name: " + application.data.toString());
                }
            }
        }
        return currentApplication;
    }

    /**
     * Retrieve Campaign Params From its Name
     *
     * @param startingMessage - Starting Message
     * @return Application
     * @throws Exception Error Exception, in failure in Network request.
     */
    public static Application getCampaignFromStartingMessage(String startingMessage){
        List<Application> applications = getApplications();

        Application currentApplication = null;
        if (applications.size() > 0) {
            for (Application application : applications) {
                try {
                    if (application.data.get("startingMessage").equals(startingMessage)) {
                        currentApplication = application;
                    }
                } catch (Exception e) {
                    log.info("Campaign has no startingMessage: " + application.data.toString());
                }
            }
        }
        return currentApplication;
    }

    public static Application getButtonLinkedApp(String appName){
        try {
            Application application = CampaignService.getCampaignFromName(appName);
            String buttonLinkedAppID = (String) ((ArrayList<Map>) application.data.get("parts")).get(0).get("buttonLinkedApp");
            Application linkedApplication = CampaignService.getCampaignFromID(buttonLinkedAppID);
            return linkedApplication;
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    private static List<Application> getApplications() {
        List<Application> applications = new ArrayList<>();
        FusionAuthClient staticClient = new FusionAuthClient("c0VY85LRCYnsk64xrjdXNVFFJ3ziTJ91r08Cm0Pcjbc", "http://134.209.150.161:9011");
        ClientResponse<ApplicationResponse, Void> response = staticClient.retrieveApplications();
        if (response.wasSuccessful()) {
            applications = response.successResponse.applications;
        } else if (response.exception != null) {
            Exception exception = response.exception;
        }
        return applications;
    }

    public static void addUserToCampaign(String phoneNumber, String appName) {
        FusionAuthClient staticClient = new FusionAuthClient("c0VY85LRCYnsk64xrjdXNVFFJ3ziTJ91r08Cm0Pcjbc", "http://134.209.150.161:9011");
        try {
            Application application = getCampaignFromGupshupAppName(appName);
            if ((boolean) application.data.get("addNewUserOnOptIn")) {
                ClientResponse<GroupResponse, Void> response = staticClient.retrieveGroups();
                if (response.wasSuccessful()) {
                    List<Group> groups = response.successResponse.groups;
                    Group groupToBeAssigned;
                    for (Group group : groups) {
                        if (group.id.toString().equals(application.data.get("group"))) {
                            groupToBeAssigned = group;
                            User user;
                            // Check if a user is there, otherwise create new
                            user = UserService.findByPhone(phoneNumber);

                            if (user == null) {
                                user = new User();
                                user.username = phoneNumber;
                                user.password = "defaultPasswordForUserNotSomeRandomString";
                                user.mobilePhone = phoneNumber;
                                UserRequest userRequest = new UserRequest(user);
                                ClientResponse<UserResponse, Errors> userResponse = staticClient.createUser(null, userRequest);
                                if (!userResponse.wasSuccessful()) {
                                    log.error("Error in creating a new user");
                                } else {
                                    log.info("New user created: " + userResponse.successResponse.user.id);
                                    assignGroupToUser(staticClient, groupToBeAssigned, userResponse.successResponse.user.id);
                                }
                            } else {
                                log.info("User already there: " + user.id);
                                assignGroupToUser(staticClient, groupToBeAssigned, user.id);
                            }
                        }else{
                            log.error("No group assigned to the campaign");
                        }
                    }
                } else if (response.exception != null) {
                    Exception exception = response.exception;
                    log.error("Error in creating a new user");
                }
            } else {
                log.info("Campaign closed; New users cannot be added" + appName);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private static void assignGroupToUser(FusionAuthClient staticClient, Group groupToBeAssigned, UUID id) {
        List<GroupMember> users = new ArrayList<>();
        GroupMember groupMember = new GroupMember();
        groupMember.userId = id;
        users.add(groupMember);
        MemberRequest memberRequest = new MemberRequest(groupToBeAssigned.id, users);
        ClientResponse<MemberResponse, Errors> memberAddedResponse = staticClient.createGroupMembers(memberRequest);
        if (memberAddedResponse.wasSuccessful()) {
            log.info("User added to campaign");
        } else {
            log.info(memberAddedResponse.errorResponse.toString());
            log.info("Failed to add user to groupId: " + groupToBeAssigned.id);
        }
    }
}
