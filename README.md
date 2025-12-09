# API Updater
Used to bulk update APIs to update the throttling policies. \
Compatible with wso2am-4.2.0. \


# Recommendations
Back up the APIM_DB and SHARED_DB before the execution of the client. 

# Building the Client 
Clone the repository. \
execute ```mvn clean install``` from inside the **/BulkAPIUpdater** directory. \
After successful build use the created **ThrottlingPolicyUpdater-1.0-SNAPSHOT-jar-with-dependencies.jar** inside **/BulkAPIUpdater/target** directory.

# Steps to follow

1. Copy the **client-truststore.jks**  file resides **<APIM_HOME>/repository/resources/security** directory and place it inside the BulkAPIUpdater client stored location 
2. Please note that ,all three files; **client-truststore.jks, ThrottlingPolicyUpdater-1.0-SNAPSHOT-jar-with-dependencies.jar, logging.properties and the config.properties** files  should be stored in the same location along with **/logs** folder.

A sample directory structure would look like follows.
```
└── /WorkingDirectory
    ├── ThrottlingPolicyUpdater-1.0-SNAPSHOT-jar-with-dependencies.jar
    ├── logging.properties
    ├── client-truststore.jks
    ├── config.properties
    └── /logs
        └── app.log
```
3. Modify the **config.properties** file accordingly - refer the sample config.properties file attached

```
# Trust-store configurations
TRUSTSTORE.PATH = client-truststore.jks
TRUSTSTORE.PASSWORD = wso2carbon

# Max API limit to be updated per request
MAX.API.LIMIT = 1000

# Tenant admin configurations
RESIDENTKM.USERNAME = admin
RESIDENTKM.PASSWORD = admin
RESIDENTKM.TOKEN.URL = https://localhost:9443/oauth2/token
RESIDENTKM.DCR.URL = https://localhost:9443/client-registration/v0.17/register
# DCR Client configurations

# Publisher REST API configurations
PUBLISHER.REST.URL = https://localhost:9443/api/am/publisher/v4/apis

# Configure thread sleep time in milliseconds between API Update
API.REDEPLOY.THREAD.SLEEP.TIME = 1000

# Configure the OLD throttling policy to be replaced
OLD.THROTTLING.POLICY = Unlimited

# Configure the NEW throttling policy
NEW.THROTTLING.POLICY = testPolicy

# Configure the API ids you want to skip as comma separated array
# Ex: API.SKIP.LIST = [a62ca2a7-a1d2-4919-9f5c-642e36d07099,352a7d6c-5bec-4964-b059-850ac6c95006]
API.SKIP.LIST = []

# Set this to true for testing with specific APIs only
ENABLE.EXPLICIT.API.UPDATE.MODE = false

# Configure the API ids you want to explicitly update (when explicit mode is enabled)
# Ex: EXPLICIT.API.UPDATE.LIST = [a62ca2a7-a1d2-4919-9f5c-642e36d07099]
EXPLICIT.API.UPDATE.LIST = []
```
4. logging.properties file

```
# Root logger configuration
handlers = java.util.logging.FileHandler
.level = INFO

# FileHandler configuration
java.util.logging.FileHandler.level = ALL
java.util.logging.FileHandler.pattern = logs/app.log
java.util.logging.FileHandler.append = true
java.util.logging.FileHandler.formatter = java.util.logging.SimpleFormatter

# SimpleFormatter custom format
java.util.logging.SimpleFormatter.format = %1$tF %1$tT | %2$s | %3$s | %5$s %n
```
# How to Run

**java -Djava.util.logging.config.file=logging.properties -jar ThrottlingPolicyUpdater-1.0-SNAPSHOT-jar-with-dependencies.jar config.properties**

**app.log** file will be created under **/logs** directory . 

Following is a sample app.log after execution 

```
2025-12-09 19:38:27 | com.sample.utilities.ConfigLoader <init> | com.sample.utilities.ConfigLoader | Configuration loaded successfully from: config.properties 
2025-12-09 19:38:28 | com.sample.updater.UpdateService main | com.sample.updater.UpdateService | ***** Starting API Throttling Policy Update ***** 
2025-12-09 19:38:28 | com.sample.updater.UpdateService main | com.sample.updater.UpdateService | ***** Number Of APIs : 2 
2025-12-09 19:38:28 | com.sample.updater.UpdateService main | com.sample.updater.UpdateService |  
2025-12-09 19:38:28 | com.sample.updater.UpdateService main | com.sample.updater.UpdateService | ***** Starting Processing API with ID :48835ead-f5b6-4633-9f2c-b81bdb945709 
2025-12-09 19:38:28 | com.sample.updater.UpdateService main | com.sample.updater.UpdateService | ***** API : PizzaShackAPI|/pizzashack|1.0.0 is in PUBLISHED State. Proceeding with Update. 
2025-12-09 19:38:28 | com.sample.utilities.RestRequest getAPIDetails | com.sample.utilities.RestRequest | urlString: https://localhost:9443/api/am/publisher/v4/apis/48835ead-f5b6-4633-9f2c-b81bdb945709 
2025-12-09 19:38:28 | com.sample.updater.UpdateService updateOperationsThrottlingPolicy | com.sample.updater.UpdateService | ***** Updated operation /order [POST] from 'Unlimited' to 'testPolicy' 
2025-12-09 19:38:28 | com.sample.updater.UpdateService updateOperationsThrottlingPolicy | com.sample.updater.UpdateService | ***** Updated operation /menu [GET] from 'Unlimited' to 'testPolicy' 
2025-12-09 19:38:28 | com.sample.updater.UpdateService updateOperationsThrottlingPolicy | com.sample.updater.UpdateService | ***** Updated operation /order/{orderId} [GET] from 'Unlimited' to 'testPolicy' 
2025-12-09 19:38:28 | com.sample.updater.UpdateService updateOperationsThrottlingPolicy | com.sample.updater.UpdateService | ***** Updated operation /order/{orderId} [PUT] from 'Unlimited' to 'testPolicy' 
2025-12-09 19:38:28 | com.sample.updater.UpdateService updateOperationsThrottlingPolicy | com.sample.updater.UpdateService | ***** Updated operation /order/{orderId} [DELETE] from 'Unlimited' to 'testPolicy' 
2025-12-09 19:38:28 | com.sample.utilities.RestRequest updateAPI | com.sample.utilities.RestRequest | API updated successfully. 
2025-12-09 19:38:28 | com.sample.updater.UpdateService main | com.sample.updater.UpdateService | ***** API throttling policy updated successfully. 
2025-12-09 19:38:28 | com.sample.updater.UpdateService handleRevisionManagement | com.sample.updater.UpdateService | ***** Revision Count for API : PizzaShackAPI|/pizzashack|1.0.0 is : 1 
2025-12-09 19:38:28 | com.sample.updater.UpdateService handleRevisionManagement | com.sample.updater.UpdateService | ***** New Revision created with id : 85970a0e-7d7a-4e42-867b-eaa06dc07939 for API : PizzaShackAPI|/pizzashack|1.0.0 
2025-12-09 19:38:28 | com.sample.updater.UpdateService handleRevisionManagement | com.sample.updater.UpdateService | ***** New Revision going to be deployed with payload : [{"revisionUuid":"6b0d1c46-4707-4787-b8d8-fbd069bd66cf","name":"Default","vhost":"localhost","displayOnDevportal":true,"deployedTime":1765289275000,"successDeployedTime":null}] 
2025-12-09 19:38:28 | com.sample.utilities.RestRequest deployRevision | com.sample.utilities.RestRequest | Successfully deployed revision 85970a0e-7d7a-4e42-867b-eaa06dc07939 
2025-12-09 19:38:28 | com.sample.updater.UpdateService main | com.sample.updater.UpdateService | ***** Completed Updating API : PizzaShackAPI|/pizzashack|1.0.0 
2025-12-09 19:38:28 | com.sample.updater.UpdateService main | com.sample.updater.UpdateService | ***** Finished Processing API with Id : 48835ead-f5b6-4633-9f2c-b81bdb945709 
2025-12-09 19:38:29 | com.sample.updater.UpdateService main | com.sample.updater.UpdateService |  
2025-12-09 19:38:29 | com.sample.updater.UpdateService main | com.sample.updater.UpdateService | ***** Starting Processing API with ID :9e5377fe-3139-4b69-8b4a-cfe6840fd5f5 
2025-12-09 19:38:29 | com.sample.updater.UpdateService main | com.sample.updater.UpdateService | ***** API : TestApi|/test|1.0.0 is in PUBLISHED State. Proceeding with Update. 
2025-12-09 19:38:29 | com.sample.utilities.RestRequest getAPIDetails | com.sample.utilities.RestRequest | urlString: https://localhost:9443/api/am/publisher/v4/apis/9e5377fe-3139-4b69-8b4a-cfe6840fd5f5 
2025-12-09 19:38:29 | com.sample.updater.UpdateService updateOperationsThrottlingPolicy | com.sample.updater.UpdateService | ***** Updated operation /* [GET] from 'Unlimited' to 'testPolicy' 
2025-12-09 19:38:29 | com.sample.updater.UpdateService updateOperationsThrottlingPolicy | com.sample.updater.UpdateService | ***** Updated operation /* [PUT] from 'Unlimited' to 'testPolicy' 
2025-12-09 19:38:29 | com.sample.updater.UpdateService updateOperationsThrottlingPolicy | com.sample.updater.UpdateService | ***** Updated operation /* [POST] from 'Unlimited' to 'testPolicy' 
2025-12-09 19:38:29 | com.sample.updater.UpdateService updateOperationsThrottlingPolicy | com.sample.updater.UpdateService | ***** Updated operation /* [DELETE] from 'Unlimited' to 'testPolicy' 
2025-12-09 19:38:29 | com.sample.updater.UpdateService updateOperationsThrottlingPolicy | com.sample.updater.UpdateService | ***** Updated operation /* [PATCH] from 'Unlimited' to 'testPolicy' 
2025-12-09 19:38:29 | com.sample.utilities.RestRequest updateAPI | com.sample.utilities.RestRequest | API updated successfully. 
2025-12-09 19:38:29 | com.sample.updater.UpdateService main | com.sample.updater.UpdateService | ***** API throttling policy updated successfully. 
2025-12-09 19:38:29 | com.sample.updater.UpdateService handleRevisionManagement | com.sample.updater.UpdateService | ***** Revision Count for API : TestApi|/test|1.0.0 is : 1 
2025-12-09 19:38:29 | com.sample.updater.UpdateService handleRevisionManagement | com.sample.updater.UpdateService | ***** New Revision created with id : 138460fc-1e15-456b-bb88-b60eb6810011 for API : TestApi|/test|1.0.0 
2025-12-09 19:38:29 | com.sample.updater.UpdateService handleRevisionManagement | com.sample.updater.UpdateService | ***** New Revision going to be deployed with payload : [{"revisionUuid":"91fc784c-a276-4e9c-a720-e138e0a24ab8","name":"Default","vhost":"localhost","displayOnDevportal":true,"deployedTime":1765289296000,"successDeployedTime":null}] 
2025-12-09 19:38:29 | com.sample.utilities.RestRequest deployRevision | com.sample.utilities.RestRequest | Successfully deployed revision 138460fc-1e15-456b-bb88-b60eb6810011 
2025-12-09 19:38:29 | com.sample.updater.UpdateService main | com.sample.updater.UpdateService | ***** Completed Updating API : TestApi|/test|1.0.0 
2025-12-09 19:38:29 | com.sample.updater.UpdateService main | com.sample.updater.UpdateService | ***** Finished Processing API with Id : 9e5377fe-3139-4b69-8b4a-cfe6840fd5f5 
2025-12-09 19:38:30 | com.sample.updater.UpdateService main | com.sample.updater.UpdateService | ***** API Throttling Policy Update Completed Successfully ***** 
```


