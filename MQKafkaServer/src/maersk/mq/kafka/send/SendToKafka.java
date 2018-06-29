package maersk.mq.kafka.send;

import java.lang.Thread.UncaughtExceptionHandler;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Timer;
import java.util.concurrent.ExecutionException;

import org.apache.kafka.common.errors.TimeoutException;
import org.json.JSONException;

import maersk.com.mqconnection.v1.MQConnection;
import maersk.com.mqconnection.v1.MQConnectionException;
import maersk.ibm.message.hub.v1.MessageHub;
import maersk.ibm.message.hub.v1.SendMsgObject;


//public class SendToKafka implements Runnable {
public class SendToKafka extends SendToKafkaBase {
	
	private byte[] message;
	
	private MQConnection mqconn;
	private MessageHub msgHub = null;

	private Timer t = null;
	
	public void setMsgHub(MessageHub msgHub) {
		this.msgHub = msgHub;
	}

	public void setMessage(byte[] message) {
		this.message = message;
	}

	public void setMqconn(MQConnection mqconn) {
		this.mqconn = mqconn;
	}

	public SendToKafka() {

		setResponseMessageStatus(false);

		Runtime.getRuntime().addShutdownHook(new Thread() 
		{

			@Override
			public void run() {
				System.out.println("Info: Shutdown received in SendToKafka ");
				
				if (msgHub != null) {
					msgHub.CloseMessageHub();
				}
				if (mqconn != null) {
					try {
						mqconn.Disconnect();
					} catch (MQConnectionException e) {
						System.out.println("Warn: Error disconnecting from MQ ");

					}
				}	
				System.out.println("Info: MQ Handler stopped cleanly");
			}
		});

	
		Thread.setDefaultUncaughtExceptionHandler(new UncaughtExceptionHandler() {		
			@Override
			public void uncaughtException(Thread t, Throwable e) {
				System.out.print("Error: UncaughtException Handler in MQ Kafka Server (1)\n");

			}
		});
		
	}
		
	
	//@Override
	public int Send(SendMsgObject msgObj) {

		String threadName = Thread.currentThread().getName();
		if (showEnv.equals("DEBUG")) {
			System.out.println("DEBUG: Sending to Kafka ");		
			Date dt = new Date();
			DateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.S");
			String date = dateFormat.format(dt);	
			System.out.println("DEBUG: Start Timestamp for " + threadName + " : " + date);
		}

		msgObj.topicName = topicName;		
	    int retCode = SendMessageToMessageHub(msgObj);
		if (retCode != 0) {
			System.out.println("Error: Not all requests were publish to topics : " + topicName);
			System.out.println("Error: ReasonCode : " + retCode);
		}
		setResponseMessageStatus(true);
		
		if (showEnv.equals("DEBUG")) {
			System.out.println("DEBUG: Message Sent to Kafka ");		
			Date dt = new Date();
			DateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.S");
			String date = dateFormat.format(dt);	
			System.out.println("DEBUG: End Timestamp for " + threadName + " : " + date);
		}
		return retCode;
		
	  }
	
	private int SendMessageToMessageHub(SendMsgObject sm) {

		//
	    int rc = 0;
		try {
			this.msgHub.SendMessage(sm);

			if (showEnv.equals("DEBUG")) {
				System.out.println("DEBUG: Message sent");
			}
			
		} catch (InterruptedException e) {
			System.out.println("Error: InterruptedException Error: " + e.getMessage());
		    rc = 1;
		    
		} catch (ExecutionException e) {
			System.out.println("Error: ExecutionException Error: " + e.getMessage());
		    rc = 2;
			
		} catch (JSONException e) {
			System.out.println("Error: JSONException Error: " + e.getMessage());
		    rc = 3;
		    
		} catch (TimeoutException e) {
			System.out.println("Error: TimeoutException Error: " + e.getMessage());
		    rc = 4;
			
		} catch (Exception e) {
			System.out.println("Error: Exception Error: " + e.getMessage());
		    rc = 5;
		}
		
		return rc;
	}

	private synchronized void setResponseMessageStatus(boolean value) {	
		this.responseSent = value;
	}
	
	/**
	 * Get the response message status
	 */
	private synchronized boolean getResponseMessageStatus() {
		return this.responseSent;
				
	}

		
}

