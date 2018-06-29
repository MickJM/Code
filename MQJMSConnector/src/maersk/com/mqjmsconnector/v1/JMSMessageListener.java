package maersk.com.mqjmsconnector.v1;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Date;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.RejectedExecutionHandler;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import javax.jms.JMSException;
import javax.jms.Message;
import javax.jms.MessageListener;
import javax.jms.Session;
import javax.jms.TextMessage;

import org.apache.kafka.common.errors.TimeoutException;
import org.json.JSONException;

//import maersk.com.mqjms.sendtokafka.v1.SendMessageBase.msgType;
//import maersk.com.mqjms.sendtokafka.v1.SendMsgObject;
import maersk.com.mqjms.sendtokafka.v1.SendToKafkaBase;
//import maersk.com.mqjms.sendtokafka.v1;
import maersk.eventhub.messaging.base.EventHubMessagingBase.msgType;
import maersk.ibm.message.hub.v1.MessageHub;
import maersk.ibm.message.hub.v1.SendMsgObject;

public class JMSMessageListener extends SendToKafkaBase implements MessageListener {

	protected boolean debug = true;
	protected boolean asyncMode;
	protected String showEnv = null;

	protected MessageHub msgHub = null;
	protected Session session = null;

    private ThreadPoolExecutor executorPool = null;

	public JMSMessageListener() {
		super();
		getEnvironmentProperties();

		int iCorePoolSize = 20;
		int iMaximumPoolSize = 30;
		long lKeepAliveTime = 10;
		int iArrayBlockingQueueSize = 10;
				
	    System.out.println("Info: Creating Threadpool and rejection handler ");	
	    RejectedExecutionHandler rejectionHandler = new RejectedExecutionHandlerImp();
	    ThreadFactory threadFactory = Executors.defaultThreadFactory();
	    this.executorPool = new ThreadPoolExecutor(iCorePoolSize,iMaximumPoolSize,lKeepAliveTime,
	    		TimeUnit.SECONDS, 
	    		new ArrayBlockingQueue<Runnable>(iArrayBlockingQueueSize), 
	    		threadFactory, rejectionHandler);
		
		
	}
	
	public void setMessageHub(MessageHub msgH) {
		this.msgHub = msgH;
	}

	public void setSession(Session sess) {
		this.session = sess;
	}

	@Override
	public void onMessage(Message msg) {
		
		String threadName = Thread.currentThread().getName();
		if (showEnv.equals("DEBUG")) {
			System.out.println("DEBUG: MQ onMessage handler = " + threadName);
			System.out.println("DEBUG: Sending to Kafka in a task");		
			Date dt = new Date();
			DateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.S");
			String date = dateFormat.format(dt);	
			System.out.println("DEBUG: Start Timestamp for " + threadName + " : " + date);
		}

		TextMessage txtMsg = (TextMessage)msg;		
		String msgId = null;
		try {
			msgId = txtMsg.getJMSMessageID();
		
		} catch (JMSException e1) {
			msgId = null;
		}
		
		System.out.println("MsgId = " + msgId);
        try {
        	
			System.out.println(txtMsg.getText());
			System.out.println("Info: Sending to Kafka ...");
			
			byte[] message = txtMsg.getText().getBytes();
			PostToKafka(message, msgId);
			
		} catch (JMSException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

        
	}

	
	private void getEnvironmentProperties() {

		try
		{
			showEnv = System.getenv("EVENTHUB_TRACE");
		}
		catch (Exception localException1) {
			showEnv = "NONE";
		}
		if (showEnv == null) {
			showEnv = "NONE";
		}
		if (showEnv.equals("DEBUG")) {
			System.out.println("Info: DEBUG is enabled");
		} else {
			System.out.println("Info: DEBUG is disabled");
		}

		
		this.asyncMode = true;
		String str2 = null;
		String envField = null;
		
		try {
			envField = "EVENTHUB_MH_SYNC_MODE";
			str2 = System.getenv(envField);
			if (showEnv.equals("DEBUG")) {
				System.out.printf("DEBUG: %s = %s\n", new Object[] { envField, str2 });
			}		       
			if (str2 != null) {
				if (str2.equals("ASYNC")) {
					this.asyncMode = true;
				} else if (str2.equals("SYNC")) {
					this.asyncMode = false;
				}
			} else {
			this.asyncMode = false;
			}
		} catch (Exception localException3) {
			this.asyncMode = false;
		}
		
		if (showEnv.equals("DEBUG")) {
			if (this.asyncMode) {
				System.out.println("DEBUG: Processing in ASYNC mode");
			} else {
				System.out.println("Info: Processing in SYNC mode");
			}
		}

	}

	private void PostToKafka(byte[] message, String msgId) throws JMSException {
		
		if (this.asyncMode) {
			PostToKafkaAsync(message, msgId);
			
		} else {
			Future<?> future = PostToKafkaSync(message);
			
			try {
				future.get();
				
			} catch (InterruptedException e) {
				Thread.currentThread().interrupt();
				
			} catch (ExecutionException e) {
			    Throwable t = e.getCause();
	            System.err.println("Error: Uncaught exception detected " + t
	                    + " st: " + Arrays.toString(t.getStackTrace()));
			}	
		}
	}

	private Future<?> PostToKafkaSync(byte[] message) {
		// TODO Auto-generated method stub
		return null;
	}

	private void PostToKafkaAsync(byte[] message, String xReqCorrelId) throws JMSException {

		SendMsgObject sm = new SendMsgObject();

		//String xReqCorrelId = msgId;
	    if (xReqCorrelId != null) {
		    sm.msgId = xReqCorrelId;
	    } else {
	    //	sm.msgId = UUID.randomUUID().toString().replaceAll("-", "");
	    	sm.msgId = null;			    
	    }

	    // Set payload and set request to bytes
	    sm.payLoad = null;
	    sm.bytePayload = message;
	    sm.type = msgType.BYTES;
	    
	    String[] topicNames = topicName.split(",");
	    int retCode = 0;
	    
		for (String topic : topicNames) { 
			sm.topicName = topic;
			retCode = SendMessageToMessageHub(sm);
			if (retCode != 0) {
				System.out.println("Error: Not all requests were publish to topics : " + topicName);
				System.out.println("Error: ReasonCode : " + retCode);
				RollBack();
				break;
			}
		}
		
		if (retCode == 0) {
			Commit();
		}
		
	}
	
	
	private int SendMessageToMessageHub(SendMsgObject sm) {

	    int rc = 0;
	    
		try {
			this.msgHub.SendMessage(sm);
			if (showEnv.equals("DEBUG")) {
				System.out.println("DEBUG: Message sent");
			}
			//this.t.cancel();
			
		} catch (InterruptedException e) {
			System.out.println("Error: InterruptedException error - " + e.getMessage());
		    rc = 1;
		    
		} catch (ExecutionException e) {
			System.out.println("Error: ExecutionException - " + e.getMessage());
		    rc = 2;
			
		} catch (JSONException e) {
			System.out.println("Error: JSONException - " + e.getMessage());
		    rc = 3;
		    
		} catch (TimeoutException e) {
			System.out.println("Error: TimeoutException - " + e.getMessage());
		    rc = 4;
			
		} catch (Exception e) {
			System.out.println("Error: Exception - " + e.getMessage());
		    rc = 5;
		}
		
	    
		return rc;
	}
	
	
	// Commit the transaction under synpoint
	public void Commit() throws JMSException {
		
		this.session.commit();
		
	}
	
	public void RollBack() throws JMSException {
		
		this.session.rollback();
	}

	
	public class RejectedExecutionHandlerImp implements RejectedExecutionHandler {

		/**
		 * Capture any rejected threads that didn't start
		 */
		@Override
		public void rejectedExecution(Runnable r, ThreadPoolExecutor executor) {
			System.out.println("Info: " + r.toString() + " is rejected");
		}
		
	}
	
}
