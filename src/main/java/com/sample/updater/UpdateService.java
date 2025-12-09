package com.sample.updater;

import com.sample.bean.API;
import com.sample.bean.Operation;
import com.sample.utilities.ConfigLoader;
import com.sample.utilities.RestRequest;
import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import javax.net.ssl.*;
import java.io.FileInputStream;
import java.security.KeyStore;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

public class UpdateService {

    private static final Logger logger = Logger.getLogger(UpdateService.class.getName());
    private static ConfigLoader configLoader;
    private static RestRequest restRequest;
    private static Gson gson = new Gson();

    public static void main(String[] args) {
        try {
            if (args.length != 1) {
                logger.log(Level.SEVERE, "Usage: java -jar UpdateClient.jar <config.properties>");
                System.exit(1);
            }

            // Load configuration
            configLoader = new ConfigLoader(args[0]);

            // Initialize SSL context
            initializeSSLContext();

            // Initialize REST request handler
            restRequest = new RestRequest(configLoader);

            // Get access token
            String accessToken = restRequest.getAccessToken();
            if (accessToken == null) {
                logger.log(Level.SEVERE, "Failed to obtain access token");
                System.exit(1);
            }

            // Get all APIs
            List<API> apis = getAllAPIs(accessToken);
            logger.log(Level.INFO, "***** Starting API Throttling Policy Update *****");
            logger.log(Level.INFO, "***** Number Of APIs : " + apis.size());

            // Get configuration for throttling policy update
            String oldThrottlingPolicy = configLoader.getProperty("OLD.THROTTLING.POLICY");
            String newThrottlingPolicy = configLoader.getProperty("NEW.THROTTLING.POLICY");
            List<String> skipList = configLoader.getListProperty("API.SKIP.LIST");
            boolean explicitMode = Boolean.parseBoolean(configLoader.getProperty("ENABLE.EXPLICIT.API.UPDATE.MODE"));
            List<String> explicitList = configLoader.getListProperty("EXPLICIT.API.UPDATE.LIST");
            int threadSleepTime = Integer.parseInt(configLoader.getProperty("API.REDEPLOY.THREAD.SLEEP.TIME"));

            // Process each API
            for (API api : apis) {
                logger.log(Level.INFO, "");
                logger.log(Level.INFO, "***** Starting Processing API with ID :" + api.getId());

                // Check if API should be skipped
                if (skipList.contains(api.getId())) {
                    logger.log(Level.INFO, "***** API : " + api.getName() + "|" + api.getContext() + "|" + api.getVersion() + " is defined in APISkipList. Hence Skipping this API");
                    logger.log(Level.INFO, "***** Finished Processing API with Id : " + api.getId());
                    continue;
                }

                // Check explicit mode
                if (explicitMode && !explicitList.contains(api.getId())) {
                    logger.log(Level.INFO, "***** Explicit mode enabled. API : " + api.getName() + "|" + api.getContext() + "|" + api.getVersion() + " is not in explicit list. Skipping.");
                    logger.log(Level.INFO, "***** Finished Processing API with Id : " + api.getId());
                    continue;
                }

                // Check API lifecycle status
                if (!"PUBLISHED".equals(api.getLifeCycleStatus())) {
                    logger.log(Level.INFO, "***** API : " + api.getName() + "|" + api.getContext() + "|" + api.getVersion() + " is not in PUBLISHED state. Current state: " + api.getLifeCycleStatus());
                    logger.log(Level.INFO, "***** Finished Processing API with Id : " + api.getId());
                    continue;
                }

                logger.log(Level.INFO, "***** API : " + api.getName() + "|" + api.getContext() + "|" + api.getVersion() + " is in PUBLISHED State. Proceeding with Update.");

                // Get full API details
                String apiDetails = restRequest.getAPIDetails(api.getId(), accessToken);
                if (apiDetails == null) {
                    logger.log(Level.SEVERE, "***** Failed to get API details for " + api.getId());
                    continue;
                }

                // Update throttling policy in operations
                String updatedApiDetails = updateOperationsThrottlingPolicy(apiDetails, oldThrottlingPolicy, newThrottlingPolicy);

                if (updatedApiDetails == null) {
                    logger.log(Level.INFO, "***** No operations found with throttling policy '" + oldThrottlingPolicy + "' for API : " + api.getName());
                    logger.log(Level.INFO, "***** Finished Processing API with Id : " + api.getId());
                    continue;
                }

                // Update the API with the modified JSON
                String updateResponse = restRequest.updateAPI(api.getId(), updatedApiDetails, accessToken);
                if (updateResponse != null) {
                    logger.log(Level.INFO, "***** API throttling policy updated successfully.");

                    // Handle revision management
                    handleRevisionManagement(api, accessToken);

                    logger.log(Level.INFO, "***** Completed Updating API : " + api.getName() + "|" + api.getContext() + "|" + api.getVersion());
                } else {
                    logger.log(Level.SEVERE, "***** Failed to update API : " + api.getName());
                }

                logger.log(Level.INFO, "***** Finished Processing API with Id : " + api.getId());

                // Sleep to avoid overwhelming the server
                Thread.sleep(threadSleepTime);
            }

            logger.log(Level.INFO, "***** API Throttling Policy Update Completed Successfully *****");

        } catch (Exception e) {
            logger.log(Level.SEVERE, "Error in main execution", e);
            System.exit(1);
        }
    }

    private static String updateOperationsThrottlingPolicy(String apiDetails, String oldPolicy, String newPolicy) {
        try {
            JsonObject apiJson = JsonParser.parseString(apiDetails).getAsJsonObject();
            JsonArray operations = apiJson.getAsJsonArray("operations");

            if (operations == null || operations.size() == 0) {
                return null;
            }

            boolean hasUpdates = false;
            for (int i = 0; i < operations.size(); i++) {
                JsonObject operation = operations.get(i).getAsJsonObject();
                if (operation.has("throttlingPolicy")) {
                    String currentPolicy = operation.get("throttlingPolicy").getAsString();
                    if (currentPolicy.equals(oldPolicy)) {
                        operation.addProperty("throttlingPolicy", newPolicy);
                        hasUpdates = true;
                        logger.log(Level.INFO, "***** Updated operation " + operation.get("target").getAsString() +
                                " [" + operation.get("verb").getAsString() + "] from '" + oldPolicy + "' to '" + newPolicy + "'");
                    }
                }
            }

            // Return the updated JSON string if there were updates, otherwise null
            return hasUpdates ? apiJson.toString() : null;

        } catch (Exception e) {
            logger.log(Level.SEVERE, "Error updating throttling policy", e);
            return null;
        }
    }

    private static List<API> getAllAPIs(String accessToken) {
        List<API> allAPIs = new ArrayList<>();
        int limit = Integer.parseInt(configLoader.getProperty("MAX.API.LIMIT"));
        int offset = 0;

        try {
            while (true) {
                String response = restRequest.getAPIs(limit, offset, accessToken);
                if (response == null) {
                    break;
                }

                JsonObject jsonResponse = JsonParser.parseString(response).getAsJsonObject();
                JsonArray apiList = jsonResponse.getAsJsonArray("list");

                if (apiList == null || apiList.size() == 0) {
                    break;
                }

                for (int i = 0; i < apiList.size(); i++) {
                    JsonObject apiJson = apiList.get(i).getAsJsonObject();
                    API api = gson.fromJson(apiJson, API.class);
                    allAPIs.add(api);
                }

                // Check pagination
                JsonObject pagination = jsonResponse.getAsJsonObject("pagination");
                if (pagination != null) {
                    int total = pagination.get("total").getAsInt();
                    if (offset + limit >= total) {
                        break;
                    }
                    offset += limit;
                } else {
                    break;
                }
            }
        } catch (Exception e) {
            logger.log(Level.SEVERE, "Error fetching APIs", e);
        }

        return allAPIs;
    }

    private static void handleRevisionManagement(API api, String accessToken) {
        try {
            // Get revision count
            int revisionCount = restRequest.getRevisionCount(api.getId(), accessToken);
            logger.log(Level.INFO, "***** Revision Count for API : " + api.getName() + "|" + api.getContext() + "|" + api.getVersion() + " is : " + revisionCount);

            // Delete oldest revision if count is 5
            if (revisionCount >= 5) {
                logger.log(Level.INFO, "***** Revision Count for API is " + revisionCount + ". Deleting Oldest Revision.");
                String oldestRevisionId = restRequest.getOldestRevisionId(api.getId(), accessToken);
                if (oldestRevisionId != null) {
                    restRequest.deleteRevision(api.getId(), oldestRevisionId, accessToken);
                }
            }

            // Create new revision
            String newRevisionId = restRequest.createRevision(api.getId(), accessToken);
            if (newRevisionId != null) {
                logger.log(Level.INFO, "***** New Revision created with id : " + newRevisionId + " for API : " + api.getName() + "|" + api.getContext() + "|" + api.getVersion());

                // Deploy revision
                String deploymentPayload = restRequest.getDeploymentEnvironments(api.getId(), accessToken);
                logger.log(Level.INFO, "***** New Revision going to be deployed with payload : " + deploymentPayload);
                restRequest.deployRevision(api.getId(), newRevisionId, deploymentPayload, accessToken);
            }

        } catch (Exception e) {
            logger.log(Level.SEVERE, "Error in revision management", e);
        }
    }

    private static void initializeSSLContext() {
        try {
            String truststorePath = configLoader.getProperty("TRUSTSTORE.PATH");
            String truststorePassword = configLoader.getProperty("TRUSTSTORE.PASSWORD");

            KeyStore trustStore = KeyStore.getInstance(KeyStore.getDefaultType());
            try (FileInputStream trustStoreStream = new FileInputStream(truststorePath)) {
                trustStore.load(trustStoreStream, truststorePassword.toCharArray());
            }

            TrustManagerFactory tmf = TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm());
            tmf.init(trustStore);

            SSLContext sslContext = SSLContext.getInstance("TLS");
            sslContext.init(null, tmf.getTrustManagers(), null);

            HttpsURLConnection.setDefaultSSLSocketFactory(sslContext.getSocketFactory());
            HttpsURLConnection.setDefaultHostnameVerifier((hostname, session) -> true);

        } catch (Exception e) {
            logger.log(Level.SEVERE, "Failed to initialize SSL context", e);
            System.exit(1);
        }
    }
}