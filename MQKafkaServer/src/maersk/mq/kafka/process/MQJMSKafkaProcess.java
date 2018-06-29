package maersk.mq.kafka.process;

import java.io.IOException;
import java.lang.Thread.UncaughtExceptionHandler;
import java.net.MalformedURLException;
import java.security.InvalidParameterException;
import java.util.Arrays;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.RejectedExecutionHandler;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import javax.jms.JMSException;

import org.apache.log4j.Level;
import org.apache.log4j.Logger;

import com.ibm.mq.MQException;
import com.ibm.mq.MQMessage;
import com.ibm.mq.constants.CMQC;
import com.ibm.mq.constants.MQConstants;

import maersk.com.mqconnection.v1.MQConnection;
import maersk.com.mqconnection.v1.MQConnectionException;
import maersk.com.mqconnection.v1.MQInvalidMessageException;
//import maersk.com.mqjmsconnector.v1.MQJMSConnector;
import maersk.com.mqparameters.v1.MQParameters;
import maersk.com.resourceproperties.v1.InvalidResourcePropertiesException;
import maersk.com.resourceproperties.v1.ResourceProperties;
import maersk.ibm.message.hub.v1.MessageHub;
import maersk.mq.kafka.process.MQKafkaProcess.RejectedExecutionHandlerImp;
import maersk.mq.kafka.send.SendToKafka;

public class MQJMSKafkaProcess extends Thread {

	private final Logger logger = Logger.getLogger(MQKafkaProcess.class);

    private ThreadPoolExecutor executorPool = null;
	private boolean shutdown = false;
	private String showEnv = null;

	private String mqQueue = null;
	//
	private ResourceProperties rp;
	private MQParameters mqp = null;

	//
	private boolean useHttpsServer = true;
	
	private MQConnection mqconn = null;
	private MessageHub msgHub = null;

	private boolean debug = true;
	protected boolean asyncMode;
	
	private boolean isStopping;
	private Object threadName;
	
	
	public boolean GetShutDown() {
		return shutdown;
	}

	public MQJMSKafkaProcess(String threadName) {
		
		this.threadName = threadName;

		getEnvironmentProperties();
		

		// Add shutdown and uncaught handlers
		Runtime.getRuntime().addShutdownHook(new Thread() 
		{
			@Override
			public void run() {
				logger.log(Level.INFO, "Shutdown received in MQKafka Listener");
				System.out.println("Info: Shutdown received in MQKafka Listener");
				shutdown = true;
			}
	
		});
	
		Thread.setDefaultUncaughtExceptionHandler(new UncaughtExceptionHandler() {		
			@Override
			public void uncaughtException(Thread t, Throwable e) {
				logger.log(Level.ERROR, "UncaughtException Handler in MQKafka Listener");
				System.out.print("ERROR: UncaughtException Handler in MQKafka Listener  ");
				e.printStackTrace();
				shutdown = true;
			}
		});

		// Create a MessageHub Kafka connection
	
		/*
		// Create an MQ Connection object
		System.out.println("Info: Connecting to MQ Server (JMS)");
		try {
			CreateMQJMSConnection();
						
		} catch (JMSException e1) {
			System.out.println("Fatal: JMSException Unable to connect to MQ Queue Manager : " 
					+ e1.getMessage() + "\nError code: " + e1.getErrorCode());
			e1.printStackTrace();
			System.exit(1);
			
		} catch (InvalidParameterException e1) {
			System.out.println("Fatal: InvalidParameter Unable to connect to MQ Queue Manager : " 
					+ e1.getMessage() );
			System.exit(1);
			
		} catch (Exception e1) {
			System.out.println("Fatal: Unable to connect to MQ Queue Manager : " 
					+ e1.getMessage() );
			System.exit(1);

		}
		*/
		

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

	
	private void CreateMQJMSConnection() throws InvalidParameterException, Exception {
	
		/*
		MQJMSConnector jmsMQ = new MQJMSConnector();
		jmsMQ.setKafkaConnection();	
		jmsMQ.setJMSConnectionProperties();
		jmsMQ.createConnection();
		*/
	
	}
	
	
	@Override
	public void run() {
		System.out.println("Info: MQ Kafka Starting");
		
	}
	


	public void StopServer() throws MQConnectionException {
		System.out.println("Info: MQ Kafka Server is stopping");
		if (this.mqconn != null) {
			this.mqconn.Disconnect();
		}
		
		if (this.executorPool != null) {
			this.executorPool.shutdown();
		}

		if (this.msgHub != null) {
			this.msgHub.CloseMessageHub();
		}
		
		this.shutdown = true;
		
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


