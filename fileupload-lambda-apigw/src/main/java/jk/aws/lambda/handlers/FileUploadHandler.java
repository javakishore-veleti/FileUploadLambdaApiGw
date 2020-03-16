package jk.aws.lambda.handlers;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.util.HashMap;
import java.util.Map;

import org.apache.commons.codec.binary.Base64;
import org.apache.commons.fileupload.FileUploadBase.FileUploadIOException;
import org.apache.commons.fileupload.MultipartStream;
import org.apache.commons.fileupload.MultipartStream.MalformedStreamException;
import org.json.simple.JSONObject;

import com.amazonaws.AmazonServiceException;
import com.amazonaws.SdkClientException;
import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.LambdaLogger;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyRequestEvent;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyResponseEvent;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.AmazonS3ClientBuilder;
import com.amazonaws.services.s3.model.ObjectMetadata;

public class FileUploadHandler implements RequestHandler<APIGatewayProxyRequestEvent, APIGatewayProxyResponseEvent> {

	@Override
	public APIGatewayProxyResponseEvent handleRequest(APIGatewayProxyRequestEvent event, Context context) {

		// Create the logger
		LambdaLogger logger = context.getLogger();
		logger.log("Loading Java Lambda handler of Proxy");

		// Log the length of the incoming body
		logger.log(String.valueOf(event.getBody().getBytes().length));

		// Create the APIGatewayProxyResponseEvent response
		APIGatewayProxyResponseEvent response = new APIGatewayProxyResponseEvent();

		// Set up contentType String
		String contentType = "";

		// Change these values to fit your region and bucket name
		String clientRegion = "us-east-1";
		String bucketName = "jk-aws-lambda-apigw-fileupload-2020mar16";

		// Every file will be named image.jpg in this example.
		// You will want to do something different here in production
		String fileObjKeyName = "image.jpg";

		try {

			byte[] bI = extractEventBodyContent(event, logger);
			byte[] boundary = extactContentTypeBoundariesFromHeaders(event, contentType);

			ByteArrayOutputStream out = writeFileContentToOutputStream(logger, bI, boundary);

			writeToS3Bucket(clientRegion, bucketName, fileObjKeyName, out, logger);

			prepareLambdaHandlerEventResponse(response);

		} catch (AmazonServiceException e) {
			logger.log(e.getMessage());
		} catch (SdkClientException e) {
			logger.log(e.getMessage());
		} catch (IOException e) {
			logger.log(e.getMessage());
		}

		logger.log(response.toString());
		return response;
	}

	private void prepareLambdaHandlerEventResponse(APIGatewayProxyResponseEvent response) {
		// Provide a response
		response.setStatusCode(200);
		Map<String, String> responseBody = new HashMap<String, String>();
		responseBody.put("Status", "File stored in S3");
		String responseBodyString = new JSONObject(responseBody).toJSONString();
		response.setBody(responseBodyString);
	}

	private byte[] extractEventBodyContent(APIGatewayProxyRequestEvent event, LambdaLogger logger)
			throws UnsupportedEncodingException {
		// Get the uploaded file and decode from base64
		byte[] bI = Base64.decodeBase64(event.getBody().getBytes());
		// Log the extraction for verification purposes
		logger.log(new String(bI, "UTF-8") + "\n");
		return bI;
	}

	private void writeToS3Bucket(String clientRegion, String bucketName, String fileObjKeyName,
			ByteArrayOutputStream out, LambdaLogger logger) {
		// Prepare an InputStream from the ByteArrayOutputStream
		InputStream fis = new ByteArrayInputStream(out.toByteArray());

		// Create our S3Client Object
		AmazonS3 s3Client = AmazonS3ClientBuilder.standard().withRegion(clientRegion).build();

		// Configure the file metadata
		ObjectMetadata metadata = new ObjectMetadata();
		metadata.setContentLength(out.toByteArray().length);
		metadata.setContentType("image/jpeg");
		metadata.setCacheControl("public, max-age=31536000");

		// Put file into S3
		s3Client.putObject(bucketName, fileObjKeyName, fis, metadata);

		// Log status
		logger.log("Put object in S3");

	}

	private ByteArrayOutputStream writeFileContentToOutputStream(LambdaLogger logger, byte[] bI, byte[] boundary)
			throws IOException, FileUploadIOException, MalformedStreamException {
		// Create a ByteArrayInputStream
		ByteArrayInputStream content = new ByteArrayInputStream(bI);

		// Create a MultipartStream to process the form-data
		MultipartStream multipartStream = new MultipartStream(content, boundary, bI.length, null);
		// Find first boundary in the MultipartStream
		boolean nextPart = multipartStream.skipPreamble();

		// Create a ByteArrayOutputStream
		ByteArrayOutputStream out = new ByteArrayOutputStream();
		// Loop through each segment
		while (nextPart) {
			String header = multipartStream.readHeaders();

			// Log header for debugging
			logger.log("Headers:");
			logger.log(header);

			// Write out the file to our ByteArrayOutputStream
			multipartStream.readBodyData(out);
			// Get the next part, if any
			nextPart = multipartStream.readBoundary();
		}

		// Log completion of MultipartStream processing
		logger.log("Data written to ByteStream");
		return out;
	}

	private byte[] extactContentTypeBoundariesFromHeaders(APIGatewayProxyRequestEvent event, String contentType) {
		// Get the content-type header and extract the boundary
		Map<String, String> hps = event.getHeaders();
		if (hps != null) {
			contentType = hps.get("content-type");
		}
		String[] boundaryArray = contentType.split("=");

		// Transform the boundary to a byte array
		byte[] boundary = boundaryArray[1].getBytes();
		return boundary;
	}

}
