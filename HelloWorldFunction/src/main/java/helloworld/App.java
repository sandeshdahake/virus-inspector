package helloworld;

import clamd.ClamavScanner;
import clamd.ClamdConfig;
import com.amazonaws.AmazonServiceException;
import com.amazonaws.SdkClientException;
import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.LambdaLogger;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyRequestEvent;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyResponseEvent;
import org.json.simple.JSONObject;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

/**
 * Handler for requests to Lambda function.
 */
public class App implements RequestHandler<APIGatewayProxyRequestEvent, APIGatewayProxyResponseEvent> {

    public APIGatewayProxyResponseEvent handleRequest(final APIGatewayProxyRequestEvent input, final Context context) {
        Map<String, String> headers = new HashMap<>();
        headers.put("Content-Type", "application/json");
        headers.put("X-Custom-Header", "application/json");

        LambdaLogger logger = context.getLogger();
        logger.log("Loading Java Lambda handler of Proxy");

        logger.log(String.valueOf(input.getBody().getBytes().length));

        APIGatewayProxyResponseEvent response = new APIGatewayProxyResponseEvent();
        ClamdConfig clamdConfig = new ClamdConfig("ec2-15-236-226-198.eu-west-3.compute.amazonaws.com",3310,50000,"20000KB","20000KB");
        ClamavScanner clamavScanner = new ClamavScanner(clamdConfig);
        try {
            clamavScanner.scan(input.getBody().getBytes());
        } catch(AmazonServiceException e) {
            // The call was transmitted successfully, but Amazon S3 couldn't
            // process it, so it returned an error response.
            logger.log(e.getMessage());
        }
        catch(SdkClientException e) {
            // Amazon S3 couldn't be contacted for a response, or the client
            // couldn't parse the response from Amazon S3.
            logger.log(e.getMessage());
        }
        catch (IOException e) {
            logger.log(e.getMessage());
        }

        response.setStatusCode(200);
        String responseBodyString = "File pass";
        response.setBody(responseBodyString);
        return response;
    }

}
