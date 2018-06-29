package maersk.mq.kafka.metrics;

import java.io.IOException;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.sql.Timestamp;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Timer;
import java.util.TimerTask;
import java.util.Map.Entry;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import com.sun.net.httpserver.Headers;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;

public class MQServerMetrics implements HttpHandler{

	private boolean responseSent;
	private MQMetrics met;
	
	public MQServerMetrics(MQMetrics m) {
		System.out.println("Info: MQ Metrics Eyecatcher");
		met = m;
	}

	@Override
	public void handle(HttpExchange http) throws IOException {

	    int rc = HttpURLConnection.HTTP_OK;
	    
		Timer t = new Timer();		
		t.schedule(
			new TimerTask() {

				@Override
				public void run() {

					if (!getResponseMessageStatus()) {
						try {
							generateErrorResponse(http,
									"Server failed to send response within specific timeout value", 
									HttpURLConnection.HTTP_GATEWAY_TIMEOUT);
						    
						} catch (Exception e) {							
					    	System.out.println("Info: (Timeout) Connection is already closed :" + e.getMessage());							
						}
					}

				}
			}, 5000
		);
		
		Object showEnv = "DEBUG";
		
		if (showEnv.equals("DEBUG")) {
			String threadName = Thread.currentThread().getName();
			System.out.println("DEBUG: Health handler threadname = " + threadName);
		}

		// Only allow POST methods
		if (!http.getRequestMethod().equalsIgnoreCase("GET")) {
			generateErrorResponse(http, "Method is not available", HttpURLConnection.HTTP_BAD_METHOD);
			t.cancel();
			return;
		}

		JSONObject jsonResp = null;
		generateResponse(http, jsonResp, HttpURLConnection.HTTP_OK);
		t.cancel();
		
		
	}

	protected synchronized void setResponseMessageStatus(boolean value) {
		this.responseSent = value;
	}

	protected synchronized boolean getResponseMessageStatus() {		
		return this.responseSent;
	}

	private void generateResponse(HttpExchange http, JSONObject jsonResp, int er) throws IOException {

		Object showEnv = "DEBUG";
		if (showEnv .equals("DEBUG")) {
			System.out.println("DEBUG: MQ Metrics eyecatcher ");
		}
		
		byte [] response = null;
		try {
			JSONObject respJson = new JSONObject();
			
			JSONArray jdata = new JSONArray();
			
			JSONObject timestamp = new JSONObject();

			// Timestamp
			Timestamp localTimestamp = new Timestamp(System.currentTimeMillis());
			timestamp.put("timestamp", localTimestamp.toString());

			JSONObject mqvalues = new JSONObject();
			
			// MQ values
			JSONObject processed = new JSONObject();
			processed.put("Processed", this.met.GetProcessed());
			//JSONObject failed = new JSONObject();
			processed.put("Failed", this.met.GetFailed());
			//JSONObject success = new JSONObject();
			processed.put("Success", this.met.GetSuccess());

			mqvalues.append("statistics", processed);
			
			jdata.put(timestamp);
			jdata.put(mqvalues);
			//jdata.put(failed );			
			//jdata.put(success);
			
			respJson.put("MQKafkaService", jdata);
	
			if (showEnv.equals("DEBUG")) {
				System.out.println("DEBUG: Json response: " + respJson.toString());
			}
			response = respJson.toString().getBytes();
			
			String st = new String(response);
			System.out.println("response = " + st);
		}
		catch (JSONException je) {
			System.out.println("Error: creating JSON error response " + je.getMessage());
		}
		if (response == null) { 
			return; 
		}
		
	    Headers reqHeaders = http.getRequestHeaders();
	    Headers respHeaders = http.getResponseHeaders();
	    
	    Iterator<Entry<String, List<String>>> it = reqHeaders.entrySet().iterator();
	    while (it.hasNext()) {
	    	Map.Entry<String, List<String>> pair = (Map.Entry<String,List<String>>)it.next();
	    	//System.out.println(pair.getKey() + " = " + pair.getValue());
	
	    	if (pair.getKey() != null) {
		    	if (pair.getKey().startsWith("X-")) {
		    		//keyvalue returns string with [ ... ]
		    		// remove the [ and ]
		    		String correl = FormatCorrelId(pair.getValue().toString());
				    respHeaders.add(pair.getKey().toString(), correl);
		    	}
	    	}
	    }
		
		
		if (showEnv.equals("DEBUG")) {
			System.out.println("DEBUG: Sending client error response ");
		}

	    //String contentType = reqHeaders.getFirst("Content-Type");
	    String contentType = "application/json";
	    respHeaders.add("Content-Type", contentType);
	    
	    http.sendResponseHeaders(er, response.length);	    
	    OutputStream os = http.getResponseBody();
	    os.write(response);
	    os.close();

	}


	private void generateErrorResponse(HttpExchange http, String msg, int er) throws IOException {

		byte [] response = null;
		try {
			response = CreateJSONErrorResponse(msg);
			System.out.println("Info : response = " + response.toString());
		}
		catch (JSONException je) {
			System.out.println("Error: creating JSON error response " + je.getMessage());
		}
		if (response == null) { 
			return; 
		}
		
	    Headers reqHeaders = http.getRequestHeaders();
	    Headers respHeaders = http.getResponseHeaders();
	    
	    Iterator<Entry<String, List<String>>> it = reqHeaders.entrySet().iterator();
	    while (it.hasNext()) {
	    	Map.Entry<String, List<String>> pair = (Map.Entry<String,List<String>>)it.next();
	    	//System.out.println(pair.getKey() + " = " + pair.getValue());
	
	    	if (pair.getKey() != null) {
		    	if (pair.getKey().startsWith("X-")) {
		    		//keyvalue returns string with [ ... ]
		    		// remove the [ and ]
		    		String correl = FormatCorrelId(pair.getValue().toString());
				    respHeaders.add(pair.getKey().toString(), correl);
		    	}
	    	}
	    }

		Object showEnv = "DEBUG";
		if (showEnv .equals("DEBUG")) {
			System.out.println("DEBUG: Sending Health client error response ");
		}

	    String contentType = "application/json";
	    respHeaders.add("Content-Type", contentType);

	    http.sendResponseHeaders(er, response.length);	    
	    OutputStream os = http.getResponseBody();
	    os.write(response);
	    os.close();

		
	}

	private byte[] CreateJSONErrorResponse(String paramString) throws JSONException {

		JSONObject localJSONObject = new JSONObject();
		localJSONObject.put("application", "MessageHub");
		    
		localJSONObject.put("description", paramString);
		Timestamp localTimestamp = new Timestamp(System.currentTimeMillis());
		localJSONObject.put("timestamp", localTimestamp.toString());
		  
		localJSONObject.put("status", "error");
		    
		return localJSONObject.toString().getBytes();
	
	}
	
	private String FormatCorrelId(String paramString) {
		String str1 = paramString.substring(0, paramString.length() - 1);
		String str2 = str1.substring(1, str1.length());
		return str2;
		
	}
	
}
