package maersk.ibm.message.hub.v1;

import org.apache.kafka.clients.producer.Callback;
import org.apache.kafka.clients.producer.RecordMetadata;
import maersk.eventhub.messaging.base.EventHubMessagingBase;

/**
 * 
 * Callback object to process Async responses from Message Hub
 *
 */
public class MessageHubProducerCallBack extends EventHubMessagingBase implements Callback {

	@Override
	public void onCompletion(RecordMetadata metadata, Exception exception) {

		if (exception != null) {
			System.out.println("Error: Error processing MessageHub ASYNC");
			System.out.println("Error: Topic     : " + metadata.topic());
			System.out.println("Error: Offset    : " + metadata.offset());
			System.out.println("Error: Partition : " + metadata.partition());
			System.out.println("Error: Timestamp : " + metadata.timestamp());
			System.out.println("Error: Exception : " + exception.getMessage());
		} else {
			if (showEnv.equals("DEBUG")) {
				System.out.println("DEBUG: Message sent *********** eyecatcher *************");
				System.out.println("DEBUG: MessageHub message");
				System.out.println("DEBUG: Topic     : " + metadata.topic());
				System.out.println("DEBUG: Offset    : " + metadata.offset());
				System.out.println("DEBUG: Partition : " + metadata.partition());
				System.out.println("DEBUG: Timestamp : " + metadata.timestamp());
				System.out.println("DEBUG: !!!!!!!!!!! eyecatcher !!!!!!!!!!!!!");
			}			
		}
	}

	
}
