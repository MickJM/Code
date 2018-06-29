package maersk.eventhub.messaging.base;

/**
 * Helper class for MessageHub producers 
 * 
 */
public class EventHubMessagingBase {

	public String showEnv = null;
	
	public enum msgType {
		STRING,
		BYTES,
		NULL
	};

	public enum envType {
		NONE,
		DEBUG,
		TRACE
	};

	public EventHubMessagingBase() {
	
		//
		try {
			this.showEnv = System.getenv("EVENTHUB_TRACE");
		}
		catch (Exception e) {
			this.showEnv = "NONE";
		}
		if (this.showEnv == null) {
			this.showEnv = "NONE";
		}
	}
}
