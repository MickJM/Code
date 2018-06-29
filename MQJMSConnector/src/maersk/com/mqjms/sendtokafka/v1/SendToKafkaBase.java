package maersk.com.mqjms.sendtokafka.v1;

import java.security.InvalidParameterException;
import java.util.Map;

public class SendToKafkaBase {

	protected String showEnv = null;
	protected int timerValue = 0; 	 
	protected boolean responseSent = false;
	protected boolean asyncMode;
	private String envField;
	protected Map<String, String> params = null;
	
	protected String topicName = null;

	public SendToKafkaBase() {

		// Trace
		try {
			showEnv = System.getenv("EVENTHUB_TRACE");
			if (showEnv.equals("DEBUG")) {
				System.out.println("Debug: DEBUG is enabled");
			}
		}
		catch (Exception e) {
			showEnv = "NONE";
		}
		if (showEnv == null) {
			showEnv = "NONE";
		}
		
		// timer
		try {
			String timerVal = System.getenv("EVENTHUB_HTTP_SERVER_TIMEOUT");
			timerValue = Integer.parseInt(timerVal);

		}
		catch (Exception e) {
			timerValue = 5000;
		}
		if (this.showEnv.equals("DEBUG")) {
			System.out.println("Debug: EVENTHUB_HTTP_SERVER_TIMEOUT is set at : " + this.timerValue);
		}

		//
		// Get the topicNames from the environment variable
		this.topicName = System.getenv("EVENTHUB_MH_TOPICNAMES");
		if (showEnv.equals("DEBUG")) {
			System.out.println("Debug: Environment variable EVENTHUB_MH_TOPICNAMES " + this.topicName);
		}		
		if (this.topicName == null) {
			System.out.println("ERROR: Environment variable EVENTHUB_MH_TOPICNAMES is not set " );
			throw new InvalidParameterException();
		}
		//
		asyncMode = true;
		String mode = null;
		try {
			envField = "EVENTHUB_MH_SYNC_MODE";
			mode = System.getenv(envField);
			if (showEnv.equals("DEBUG")) {
				System.out.printf("Debug:: %s = %s\n", envField, mode);
			}

			if (mode != null) {
				if (mode.equals("ASYNC")) {
					asyncMode = true;
				} else if (mode.equals("SYNC")) {
					asyncMode = false;
				}
			} else {
				asyncMode = false;
			}
		}
		catch (Exception e) {
			asyncMode = false;
		}
		if (showEnv.equals("DEBUG")) {
			if (asyncMode) {
				System.out.println("Debug: Processing in ASYNC mode");
			} else {
				System.out.println("Info: Processing in SYNC mode");
			}
		}

		
		
	}
	
	
}
