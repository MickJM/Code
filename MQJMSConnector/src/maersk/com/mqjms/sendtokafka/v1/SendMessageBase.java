package maersk.com.mqjms.sendtokafka.v1;

public class SendMessageBase {

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

	public SendMessageBase() {
	
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
