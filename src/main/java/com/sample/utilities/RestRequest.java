package com.sample.utilities;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.logging.Level;
import java.util.logging.Logger;

public class RestRequest {

    private static final Logger logger = Logger.getLogger(RestRequest.class.getName());
    private final ConfigLoader configLoader;
    private final String publisherRestURL;
    private final String tokenURL;
    private final String clientRegistrationURL;
    private final String adminUsername;
    private final String adminPassword;
    public static class ClientCredentials {
        public String clientId;
        public String clientSecret;
    }

    public RestRequest(ConfigLoader configLoader) {
        this.configLoader = configLoader;
        this.publisherRestURL = configLoader.getProperty("PUBLISHER.REST.URL");
        this.tokenURL = configLoader.getProperty("RESIDENTKM.TOKEN.URL");
        this.clientRegistrationURL = configLoader.getProperty("RESIDENTKM.DCR.URL");
        this.adminUsername = configLoader.getProperty("RESIDENTKM.USERNAME");
        this.adminPassword = configLoader.getProperty("RESIDENTKM.PASSWORD");
    }
    public ClientCredentials registerClient() {
        ClientCredentials credentials = new ClientCredentials();

        try {

            URL url = new URL(clientRegistrationURL);
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("POST");

            conn.setRequestProperty("Content-Type", "application/json");

            // Basic admin authentication
            String auth = adminUsername + ":" + adminPassword;
            String encodedAuth = Base64.getEncoder().encodeToString(auth.getBytes(StandardCharsets.UTF_8));
            conn.setRequestProperty("Authorization", "Basic " + encodedAuth);

            conn.setDoOutput(true);

            // Minimal JSON payload required by DCR API
            String body = "{\n" +
                    "  \"callbackUrl\": \"www.google.lk\",\n" +
                    "  \"clientName\": \"rest_api_publisher\",\n" +
                    "  \"owner\": \"" + adminUsername + "\",\n" +
                    "  \"grantType\": \"password client_credentials refresh_token\"\n" +
                    "}";

            try (OutputStream os = conn.getOutputStream()) {
                os.write(body.getBytes(StandardCharsets.UTF_8));
            }

            int code = conn.getResponseCode();

            if (code == 200 || code == 201) {
                String response = readResponse(conn.getInputStream());
                JsonObject json = JsonParser.parseString(response).getAsJsonObject();

                credentials.clientId = json.get("clientId").getAsString();
                credentials.clientSecret = json.get("clientSecret").getAsString();

                return credentials;
            } else {
                logger.log(Level.SEVERE, "Failed to register client. Code: " + code);
            }
        } catch (Exception e) {
            logger.log(Level.SEVERE, "Error registering client", e);
        }

        return null;
    }

    public String getAccessToken() {
        try {
            ClientCredentials creds = registerClient();  // NEW LINE

            if (creds == null) {
                logger.log(Level.SEVERE, "DCR client registration failed. Cannot continue.");
                return null;
            }

            URL url = new URL(tokenURL);

            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("POST");
            conn.setRequestProperty("Content-Type", "application/x-www-form-urlencoded");

            // Use DCR client credentials
            String auth = creds.clientId + ":" + creds.clientSecret;
            String encodedAuth = Base64.getEncoder().encodeToString(auth.getBytes(StandardCharsets.UTF_8));
            conn.setRequestProperty("Authorization", "Basic " + encodedAuth);

            conn.setDoOutput(true);

            String requestBody = "grant_type=password&username=" + adminUsername +
                    "&password=" + adminPassword +
                    "&scope=apim:api_view apim:api_create apim:api_manage";

            try (OutputStream os = conn.getOutputStream()) {
                os.write(requestBody.getBytes(StandardCharsets.UTF_8));
            }

            int responseCode = conn.getResponseCode();
            if (responseCode == 200) {
                String response = readResponse(conn.getInputStream());
                JsonObject json = JsonParser.parseString(response).getAsJsonObject();
                return json.get("access_token").getAsString();
            } else {
                logger.log(Level.SEVERE, "Failed to get access token. Response code: " + responseCode);
            }

        } catch (Exception e) {
            logger.log(Level.SEVERE, "Error getting access token", e);
        }
        return null;
    }


    public String getAPIs(int limit, int offset, String accessToken) {
        try {
            String urlString = publisherRestURL + "?limit=" + limit + "&offset=" + offset;
            URL url = new URL(urlString);
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("GET");
            conn.setRequestProperty("Authorization", "Bearer " + accessToken);
            conn.setRequestProperty("Content-Type", "application/json");

            int responseCode = conn.getResponseCode();
            if (responseCode == 200) {
                return readResponse(conn.getInputStream());
            } else {
                logger.log(Level.SEVERE, "Failed to get APIs. Response code: " + responseCode);
                return null;
            }
        } catch (Exception e) {
            logger.log(Level.SEVERE, "Error getting APIs", e);
            return null;
        }
    }

    public String getAPIDetails(String apiId, String accessToken) {
        try {
            String urlString = publisherRestURL + "/" + apiId;
            logger.log(Level.INFO, "urlString: " + urlString);

            URL url = new URL(urlString);
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("GET");
            conn.setRequestProperty("Authorization", "Bearer " + accessToken);
            conn.setRequestProperty("Content-Type", "application/json");

            int responseCode = conn.getResponseCode();
            if (responseCode == 200) {
                return readResponse(conn.getInputStream());
            } else {
                logger.log(Level.SEVERE, "Failed to get API details for " + apiId + ". Response code: " + responseCode);
                return null;
            }
        } catch (Exception e) {
            logger.log(Level.SEVERE, "Error getting API details", e);
            return null;
        }
    }

    public String updateAPI(String apiId, String apiPayload, String accessToken) {
        try {
            String urlString = publisherRestURL + "/" + apiId;
            URL url = new URL(urlString);
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("PUT");
            conn.setRequestProperty("Authorization", "Bearer " + accessToken);
            conn.setRequestProperty("Content-Type", "application/json");
            conn.setDoOutput(true);

            try (OutputStream os = conn.getOutputStream()) {
                os.write(apiPayload.getBytes(StandardCharsets.UTF_8));
            }

            int responseCode = conn.getResponseCode();
            if (responseCode == 200) {
                logger.log(Level.INFO, "API updated successfully.");
                return readResponse(conn.getInputStream());
            } else {
                String errorResponse = readResponse(conn.getErrorStream());
                logger.log(Level.SEVERE, "Failed to update API. Response code: " + responseCode + ", Error: " + errorResponse);
                return null;
            }
        } catch (Exception e) {
            logger.log(Level.SEVERE, "Error updating API", e);
            return null;
        }
    }

    public int getRevisionCount(String apiId, String accessToken) {
        try {
            String urlString = publisherRestURL + "/" + apiId + "/revisions";
            URL url = new URL(urlString);
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("GET");
            conn.setRequestProperty("Authorization", "Bearer " + accessToken);
            conn.setRequestProperty("Content-Type", "application/json");

            int responseCode = conn.getResponseCode();
            if (responseCode == 200) {
                String response = readResponse(conn.getInputStream());
                JsonObject jsonResponse = JsonParser.parseString(response).getAsJsonObject();
                return jsonResponse.get("count").getAsInt();
            }
        } catch (Exception e) {
            logger.log(Level.SEVERE, "Error getting revision count", e);
        }
        return 0;
    }

    public String getOldestRevisionId(String apiId, String accessToken) {
        try {
            String urlString = publisherRestURL + "/" + apiId + "/revisions";
            URL url = new URL(urlString);
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("GET");
            conn.setRequestProperty("Authorization", "Bearer " + accessToken);
            conn.setRequestProperty("Content-Type", "application/json");

            int responseCode = conn.getResponseCode();
            if (responseCode == 200) {
                String response = readResponse(conn.getInputStream());
                JsonObject jsonResponse = JsonParser.parseString(response).getAsJsonObject();
                JsonArray revisions = jsonResponse.getAsJsonArray("list");

                if (revisions != null && revisions.size() > 0) {
                    // Get the oldest revision (first one in the list)
                    JsonObject oldestRevision = revisions.get(0).getAsJsonObject();
                    return oldestRevision.get("id").getAsString();
                }
            }
        } catch (Exception e) {
            logger.log(Level.SEVERE, "Error getting oldest revision", e);
        }
        return null;
    }

    public void deleteRevision(String apiId, String revisionId, String accessToken) {
        try {
            String urlString = publisherRestURL + "/" + apiId + "/revisions/" + revisionId;
            URL url = new URL(urlString);
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("DELETE");
            conn.setRequestProperty("Authorization", "Bearer " + accessToken);
            conn.setRequestProperty("Content-Type", "application/json");

            int responseCode = conn.getResponseCode();
            if (responseCode == 200 || responseCode == 204) {
                logger.log(Level.INFO, "Successfully deleted revision ID: " + revisionId + " for API ID: " + apiId);
            } else {
                logger.log(Level.SEVERE, "Failed to delete revision. Response code: " + responseCode);
            }
        } catch (Exception e) {
            logger.log(Level.SEVERE, "Error deleting revision", e);
        }
    }

    public String createRevision(String apiId, String accessToken) {
        try {
            String urlString = publisherRestURL + "/" + apiId + "/revisions";
            URL url = new URL(urlString);
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("POST");
            conn.setRequestProperty("Authorization", "Bearer " + accessToken);
            conn.setRequestProperty("Content-Type", "application/json");
            conn.setDoOutput(true);

            String requestBody = "{\"description\":\"Throttling policy update\"}";
            try (OutputStream os = conn.getOutputStream()) {
                os.write(requestBody.getBytes(StandardCharsets.UTF_8));
            }

            int responseCode = conn.getResponseCode();
            if (responseCode == 200 || responseCode == 201) {
                String response = readResponse(conn.getInputStream());
                JsonObject jsonResponse = JsonParser.parseString(response).getAsJsonObject();
                return jsonResponse.get("id").getAsString();
            } else {
                logger.log(Level.SEVERE, "Failed to create revision. Response code: " + responseCode);
                return null;
            }
        } catch (Exception e) {
            logger.log(Level.SEVERE, "Error creating revision", e);
            return null;
        }
    }

    public String getDeploymentEnvironments(String apiId, String accessToken) {
        try {
            String urlString = publisherRestURL + "/" + apiId + "/deployments";
            URL url = new URL(urlString);
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("GET");
            conn.setRequestProperty("Authorization", "Bearer " + accessToken);
            conn.setRequestProperty("Content-Type", "application/json");

            int responseCode = conn.getResponseCode();
            if (responseCode == 200) {
                String response = readResponse(conn.getInputStream());
                JsonArray environments = JsonParser.parseString(response).getAsJsonArray();
                return environments.toString();
            }
        } catch (Exception e) {
            logger.log(Level.SEVERE, "Error getting deployment environments", e);
        }
        return "[]";
    }

    public void deployRevision(String apiId, String revisionId, String deploymentPayload, String accessToken) {
        try {
            String urlString = publisherRestURL + "/" + apiId + "/deploy-revision?revisionId=" + revisionId;
            URL url = new URL(urlString);
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("POST");
            conn.setRequestProperty("Authorization", "Bearer " + accessToken);
            conn.setRequestProperty("Content-Type", "application/json");
            conn.setDoOutput(true);

            try (OutputStream os = conn.getOutputStream()) {
                os.write(deploymentPayload.getBytes(StandardCharsets.UTF_8));
            }

            int responseCode = conn.getResponseCode();
            if (responseCode == 200 || responseCode == 201) {
                logger.log(Level.INFO, "Successfully deployed revision " + revisionId);
            } else {
                logger.log(Level.SEVERE, "Failed to deploy revision. Response code: " + responseCode);
            }
        } catch (Exception e) {
            logger.log(Level.SEVERE, "Error deploying revision", e);
        }
    }

    private String readResponse(InputStream inputStream) throws IOException {
        if (inputStream == null) {
            return "";
        }

        StringBuilder response = new StringBuilder();
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream, StandardCharsets.UTF_8))) {
            String line;
            while ((line = reader.readLine()) != null) {
                response.append(line);
            }
        }
        return response.toString();
    }
}