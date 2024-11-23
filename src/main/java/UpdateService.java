import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import utilities.ReadConfigFile;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Base64;
import java.util.logging.Level;
import java.util.logging.Logger;

import static utilities.RestRequest.*;

public class UpdateService {

    private static final Logger LOGGER = Logger.getLogger(UpdateService.class.getName());

    public static void main(String[] args) {
        ReadConfigFile configs = new ReadConfigFile();

        String adminusername = configs.getProperty("ADMIN.USERNAME");
        String adminpassword = configs.getProperty("ADMIN.PASSWORD");
        String publisherRestUrl = configs.getProperty("PUBLISHER.REST.URL");
        String visibilityRestrictRole = configs.getProperty("DEVPORTAL.RESTRICTED.ROLE");
        String apiSkipList = configs.getProperty("API.SKIP.LIST");
        String [] apiSkipListArray = getApiSkipListArray(apiSkipList);
        long sleepTime = Long.parseLong(configs.getProperty("API.REDEPLOY.THREAD.SLEEP.TIME"));

        LOGGER.info("***** Starting API Update *****");

        // Create Basic accessToken by Encoding admin credentials
        String accessToken = Base64.getEncoder().encodeToString((adminusername + ":" + adminpassword).getBytes(StandardCharsets.UTF_8));

        // Get API List by calling /api/am/publisher/v3/apis
        ArrayList<JSONObject> apiDetailsArray = getAPIList(publisherRestUrl, accessToken);

        if (apiDetailsArray != null && !apiDetailsArray.isEmpty()) {
            LOGGER.info("***** Number Of APIs : " + apiDetailsArray.size());
            LOGGER.info("");

            // Iterate through each API in the list
            for (JSONObject apiDetails : apiDetailsArray) {
                String apiId = (String) apiDetails.get("id");
                LOGGER.info("***** Starting Processing API with ID :" + apiId);
                String apiName = (String) apiDetails.get("name");
                String apiContext = (String) apiDetails.get("context");
                String apiVersion = (String) apiDetails.get("version");
                String apiStatus = (String) apiDetails.get("lifeCycleStatus");

                // If defined in apiSkipList skip this API
                if (Arrays.asList(apiSkipListArray).contains(apiId)) {
                    LOGGER.info("***** API : " + apiName + "|" + apiContext + "|" + apiVersion + " is defined in APISkipList. Hence Skipping this API ");
                    LOGGER.info("***** Finished Processing API with Id : " + apiId);
                    LOGGER.info("");
                    continue;
                }
                // We only update the APIs which are in published state
                if (!apiStatus.equalsIgnoreCase("published")) {
                    LOGGER.info("***** API : " + apiName + "|" + apiContext + "|" + apiVersion + " is Not in PUBLISHED State. Currently in state: " + apiStatus + " - Skipping this API.");
                } else {
                    LOGGER.info("***** API : " + apiName + "|" + apiContext + "|" + apiVersion + " is in PUBLISHED State. Proceeding with Update.");

                    try {
                        // Get API details by passing the apiId by calling /api/am/publisher/v3/apis/<apiId>
                        JSONObject apiDetailsByApiId = getAPIDetailsByApiId(publisherRestUrl, accessToken, apiId);

                        // Check if devportal visibility is PUBLIC
                        String apiVisibility = (String) apiDetailsByApiId.get("visibility");

                        // Modify the API visibility to restricted if it's public
                        if (apiVisibility.equalsIgnoreCase("public")) {
                            apiDetailsByApiId.put("visibility", "RESTRICTED");
                            JSONArray visibleRoles = (JSONArray) apiDetailsByApiId.get("visibleRoles");

                            if (visibleRoles == null) {
                                visibleRoles = new JSONArray(); // initialize if empty
                            }

                            if (!visibleRoles.contains(visibilityRestrictRole)) {
                                visibleRoles.add(visibilityRestrictRole);
                            }

                            apiDetailsByApiId.put("visibleRoles", visibleRoles);

                            // Call update API
                            boolean isUpdated = updateApi(publisherRestUrl, accessToken, apiId, apiDetailsByApiId);
                            if (isUpdated) {
                                LOGGER.info("***** API visibility and roles updated successfully.");
                            } else {
                                LOGGER.warning("***** Failed to update API visibility and roles.");
                            }

                        } else if (apiVisibility.equalsIgnoreCase("restricted")) {
                            // Handle only visibleRoles for restricted visibility
                            JSONArray visibleRoles = (JSONArray) apiDetailsByApiId.get("visibleRoles");
                            if (visibleRoles == null) {
                                visibleRoles = new JSONArray();
                            }

                            if (!visibleRoles.contains(visibilityRestrictRole)) {
                                visibleRoles.add(visibilityRestrictRole);
                            }

                            apiDetailsByApiId.put("visibleRoles", visibleRoles);

                            // Call update API
                            boolean isUpdated = updateApi(publisherRestUrl, accessToken, apiId, apiDetailsByApiId);
                            if (isUpdated) {
                                LOGGER.info("***** API visibility and roles updated successfully.");
                            } else {
                                LOGGER.warning("***** Failed to update API visibility and roles.");
                            }
                        }

                        // Get revision list by passing API ID
                        ArrayList<JSONObject> apiRevisionArray = getRevisionListByApiId(publisherRestUrl, accessToken, apiId);
                        LOGGER.info("***** Revision Count for API : " + apiName + "|" + apiContext + "|" + apiVersion + " is : " + apiRevisionArray.size());

                        if (apiRevisionArray.size() < 5) {
                            // Create new API revision
                            JSONObject newApiRevision = createRevision(publisherRestUrl, accessToken, apiId);
                            String newApiRevisionId = (String) newApiRevision.get("id");
                            LOGGER.info("***** New Revision created with id : " + newApiRevisionId + " for API : " + apiName + "|" + apiContext + "|" + apiVersion);

                            // Create payload to deploy new API revision
                            ArrayList<JSONObject> deployRevisionPayload = buildDeployRevisionPayload(apiRevisionArray);
                            LOGGER.info("***** New Revision going to be deployed with payload : " + deployRevisionPayload.toString());

                            // Deploy new API revision
                            ArrayList<JSONObject> newDeployedRevisionDetails = deployRevision(publisherRestUrl, accessToken, apiId, newApiRevisionId, deployRevisionPayload);
                            Thread.sleep(sleepTime);

                        } else {
                            LOGGER.info("***** Revision Count for API is 5. Deleting Oldest Revision.");

                            // Find revision to delete
                            String revisionIdToDelete = findRevisionToDelete(apiRevisionArray);

                            // Delete revision and get remaining revisions
                            ArrayList<JSONObject> remainingApiRevisionArray = deleteRevision(publisherRestUrl, accessToken, apiId, revisionIdToDelete);

                            // Create new API revision
                            JSONObject newApiRevision = createRevision(publisherRestUrl, accessToken, apiId);
                            String newApiRevisionId = (String) newApiRevision.get("id");
                            LOGGER.info("***** New Revision created with id : " + newApiRevisionId + " for API : " + apiName + "|" + apiContext + "|" + apiVersion);

                            // Create payload to deploy new API revision
                            ArrayList<JSONObject> deployRevisionPayload = buildDeployRevisionPayload(remainingApiRevisionArray);
                            LOGGER.info("***** New Revision going to be deployed with payload : " + deployRevisionPayload.toString());

                            // Deploy new API revision
                            ArrayList<JSONObject> newDeployedRevisionDetails = deployRevision(publisherRestUrl, accessToken, apiId, newApiRevisionId, deployRevisionPayload);
                            Thread.sleep(sleepTime);
                        }

                        LOGGER.info("***** Completed Updating API : " + apiName + "|" + apiContext + "|" + apiVersion);
                    } catch (Exception e) {
                        LOGGER.log(Level.SEVERE, "Error occurred while processing API with ID: " + apiId, e);
                        LOGGER.log(Level.SEVERE, "Stopping Execution Of Client");
                        System.exit(0);
                    }
                }

                LOGGER.info("***** Finished Processing API with Id : " + apiId);
                LOGGER.info("");

                try {
                    Thread.sleep(sleepTime);
                } catch (InterruptedException e) {
                    LOGGER.log(Level.SEVERE, "Thread sleep interrupted", e);
                }
            }
        }

        LOGGER.info("............ API updating completed ............ ");
    }

    public static String findRevisionToDelete(ArrayList<JSONObject> apiRevisionArray) {
        String revisionIdToDelete = null;
        long oldestCreatedTime = Long.MAX_VALUE;

        for (JSONObject revision : apiRevisionArray) {
            long createdTime = (long) revision.get("createdTime");
            ArrayList<?> deploymentInfo = (ArrayList<?>) revision.get("deploymentInfo");

            if (deploymentInfo.isEmpty() && createdTime < oldestCreatedTime) {
                oldestCreatedTime = createdTime;
                revisionIdToDelete = (String) revision.get("id");
            }
        }
        return revisionIdToDelete;
    }

    public static ArrayList<JSONObject> buildDeployRevisionPayload(ArrayList<JSONObject> apiRevisionArray) {
        ArrayList<JSONObject> payload = new ArrayList<>();
        for (JSONObject revision : apiRevisionArray) {
            JSONArray deploymentInfo = (JSONArray) revision.get("deploymentInfo");
            if (deploymentInfo != null) {
                for (Object obj : deploymentInfo) {
                    JSONObject deployment = (JSONObject) obj;
                    JSONObject formattedDeployment = new JSONObject();
                    formattedDeployment.put("name", deployment.get("name"));
                    formattedDeployment.put("vhost", deployment.get("vhost"));
                    formattedDeployment.put("displayOnDevportal", deployment.get("displayOnDevportal"));
                    payload.add(formattedDeployment);
                }
            }
        }
        return payload;
    }

    public static String[] getApiSkipListArray(String apiSkipList) {
        if (apiSkipList == null || apiSkipList.isEmpty()) {
            return new String[0];  // Return an empty array if input is invalid
        }
        apiSkipList = apiSkipList.trim();
        if (apiSkipList.startsWith("[") && apiSkipList.endsWith("]")) {
            // Remove the surrounding brackets
            apiSkipList = apiSkipList.substring(1, apiSkipList.length() - 1).trim();
        } else {
            throw new IllegalArgumentException("Input must be a list formatted with square brackets.");
        }
        // Split by comma and trim spaces from each element
        String[] array = apiSkipList.split(",");
        for (int i = 0; i < array.length; i++) {
            array[i] = array[i].trim();
        }
        return array;
    }
}
