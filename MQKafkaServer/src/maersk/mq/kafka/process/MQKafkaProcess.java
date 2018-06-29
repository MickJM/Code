package maersk.mq.kafka.process;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.MalformedURLException;
import java.security.InvalidParameterException;
import java.text.DateFormat;
import java.text.SimpleDateFormat;

import org.apache.log4j.Level;
import org.apache.log4j.Logger;

import com.ibm.mq.MQException;
import com.ibm.mq.MQMessage;
import com.ibm.mq.constants.CMQC;
import com.ibm.mq.constants.MQConstants;
import com.sun.net.httpserver.HttpServer;

import maersk.com.mqconnection.v1.MQConnection;
import maersk.com.mqconnection.v1.MQConnectionException;
import maersk.com.mqconnection.v1.MQInvalidMessageException;
import maersk.com.mqparameters.v1.MQParameters;
import maersk.com.resourceproperties.v1.InvalidResourcePropertiesException;
import maersk.com.resourceproperties.v1.ResourceProperties;
import maersk.eventhub.messaging.base.EventHubMessagingBase.msgType;
import maersk.ibm.message.hub.v1.MessageHub;
import maersk.ibm.message.hub.v1.SendMsgObject;
import maersk.mq.kafka.metrics.MQMetrics;
import maersk.mq.kafka.metrics.MQServerMetrics;
import maersk.mq.kafka.send.SendToKafka;

import java.util.Date;
import java.util.concurrent.* ;

public class MQKafkaProcess extends Thread {

	private final Logger logger = Logger.getLogger(MQKafkaProcess.class);

    private ThreadPoolExecutor executorPool = null;
    private ScheduledExecutorService schedexecutorPool = null;

	private boolean shutdown = false;
	private String showEnv = null;
	//
	private ResourceProperties rp;
	private MQParameters mqp = null;
	//	
	private MQConnection mqconn = null;
	private MessageHub msgHub = null;

	protected boolean asyncMode;
	protected int timerValue = 0;
	
	private boolean isStopping;
	private Object threadName;
	
	private MQMetrics met = null;
	
	private SendToKafka stk = null;

	private HttpServer httpServer;
	
	public boolean GetShutDown() {
		return shutdown;
	}

	/*
	 * MQKafka class
	 */
	public MQKafkaProcess(String threadName) throws MQConnectionException {
				
		this.threadName = threadName;
		getEnvironmentProperties ();
		isStopping = false;
		
		// Add shutdown and uncaught handlers
		Runtime.getRuntime().addShutdownHook(new Thread() 
		{
			@Override
			public void run() {
				logger.log(Level.INFO, "Shutdown received in MQKafka Listener");
				System.out.println("Info: Shutdown received in MQKafka Listener");
				//
				try {
					StopServer();

				} catch (MQConnectionException e) {
					e.printStackTrace();
					System.out.println("Error: Error stopping Server (1) " + e.GetMessage());
					System.exit(1);
				}
				
			}
	
		});
	
		Thread.setDefaultUncaughtExceptionHandler(new UncaughtExceptionHandler() {		
			@Override
			public void uncaughtException(Thread t, Throwable e) {
				logger.log(Level.ERROR, "UncaughtException Handler in MQKafka Listener");
				System.out.print("ERROR: UncaughtException Handler in MQKafka Listener  ");
				e.printStackTrace();
				try {
					StopServer();

				} catch (MQConnectionException e1) {
					e.printStackTrace();
					System.out.println("Error: Error stopping Server (2) " + e1.GetMessage());
					System.exit(1);
				}
			}
		});

		// Create a MessageHub Kafka connection		
		try {
			CreateKafkaConnection();
			
		} catch (Exception e) {
			System.out.println("Fatal: Unable to connect to MessageHub : " + e.getMessage());
			System.exit(1);			
		}

		
		// Create a queue manager connection
		try {
			CreateMQConnection();
			
		} catch (MQConnectionException e1) {
			System.out.println("Fatal: Unable to connect to MQ Queue Manager : " 
						+ e1.GetMessage());
			System.out.println("Info: CompletationCode : " + e1.GetCompletionCode()
					+ " Reason: " + e1.GetReasonCode()
					+ " Message: " + e1.GetMessage());
			
			try {
				throw new MQException(e1.GetCompletionCode(), e1.GetReasonCode(), e1.GetMessage());
			
			} catch (MQException e2) {
				System.exit(1);
			}
		} catch (MalformedURLException e1) {
			System.out.println("Fatal: Unable to connect to MQ Queue Manager : " 
					+ rp.GetMQQueueManager());
			StopServer();
		}

	}

	
	// Create a queue manager connection 
	private void CreateMQConnection() throws MQConnectionException, MalformedURLException {

		// Load the MQ properties
		System.out.println("Info: Loading MQ properties");
		getMQProperties();

		// Create an MQ connection object 
		this.mqconn = new MQConnection();
		this.mqconn.setUseCCDT(this.mqp.GetUsingCCDT());
		this.mqconn.setUseSSL(this.mqp.GetUseSSLChannel());
		

		System.out.println("Info: Connecting to MQ Server " + rp.GetMQQueueManager());
		//if (!this.mqconn.isUseCCDT()) {
			this.mqconn.SetConnectionParameters(mqp);			
		//}
		
		// Create a new connection
		this.mqconn.CreateConnection();	
		if (showEnv.equals("DEBUG")) {
			logger.log(Level.INFO, "MQKafkaProcess: Connected to queue manager - " + rp.GetMQQueueManager());
		}
		System.out.println("Info: Connected to MQ Server " + rp.GetMQQueueManager());;
	}

	/* 
	 * Create a Kafka connection
	 */
	private void CreateKafkaConnection() throws InvalidParameterException, Exception {
	
		System.out.println("Info: Creating MessageHub (Kafka) connection " );
		if (this.msgHub == null) {
			this.msgHub = new MessageHub();
		}
		System.out.println("Info: MessageHub connection created " );
		
	}
	
	/*
	 * Main processing method
	 * 
	 */
	@Override
	public void run() {

		this.met = new MQMetrics();
		
		//
		try {
			this.httpServer = HttpServer.create(new InetSocketAddress(8085), 0);
			this.httpServer.createContext("/maersk/mq/metrics", new MQServerMetrics(met));    
		    this.httpServer.start();

		} catch (IOException e2) {
			// TODO Auto-generated catch block
			System.out.println("Info: Unable to create HTTP Server ");
		}
		

		//
		System.out.println("Info: MQKafka server starting");
		try {
			ProcessMQMessages();
			if (showEnv.equals("DEBUG")) {
				logger.log(Level.INFO, "Processing ending");
			}

		} catch (MQInvalidMessageException e) {
			System.out.println("Fatal: MQInvalidMessageException");
			System.out.println("CompletionCode: " + e.GetCompletionCode() 
			+ " ReasonCode: " + e.GetReasonCode() 
			+ " Desc: " + e.GetMessage());
			
			logger.log(Level.FATAL, "MQInvalidMessageException");
			logger.log(Level.FATAL, "CompletionCode: " + e.GetCompletionCode() 
			+ " ReasonCode: " + e.GetReasonCode() 
			+ " Desc: " + e.GetMessage());
			
			try {
				StopServer();
				
			} catch (MQConnectionException e1) {
				// Dont do anything
			}
			System.exit(1);
			
		} catch (IOException e) {
			System.out.println("Fatal: IOException");
			System.out.println("Desc: " + e.getMessage()); 

			logger.log(Level.FATAL, "IOException");
			logger.log(Level.FATAL, " Desc: " + e.getMessage());
			try {
				StopServer();
				
			} catch (MQConnectionException e1) {
				// Dont do anything
			}
			System.exit(1);

		} catch (InterruptedException e) {
			System.out.println("Fatal: InterruptedException");
			System.out.println("Desc: " + e.getMessage()); 

			logger.log(Level.FATAL, "InterruptedException");
			logger.log(Level.FATAL, " Desc: " + e.getMessage());
			try {
				StopServer();
				
			} catch (MQConnectionException e1) {
				// Dont do anything
			}
			System.exit(1);

		} catch (MQException e) {
			System.out.println("Fatal: MQException");
			System.out.println("Desc: " + e.getMessage()); 

			logger.log(Level.FATAL, "MQException");
			logger.log(Level.FATAL, " Desc: " + e.getMessage());
			try {
				StopServer();
				
			} catch (MQConnectionException e1) {
				// Dont do anything
			}
			System.exit(1);
		}	
	}
	
	/*
	 * Process messages
	 */
	private void ProcessMQMessages() throws 
			MQInvalidMessageException, IOException, InterruptedException, MQException {

		// Get backout queue and threshold values
		String queueName = rp.GetMQQueue();		
		int[] query = {CMQC.MQIA_BACKOUT_THRESHOLD, CMQC.MQCA_BACKOUT_REQ_Q_NAME };
		int[] outi = new int[1];
		byte[] outb = new byte[48];
		this.mqconn.InquireQueue(query, queueName, outi, outb);
		int iBackoutThreshHold = this.mqconn.getBackoutThreshHoldCount();
		String sBackoutQueue = this.mqconn.getBackoutThreshHoldQueue().trim();
		
		if (showEnv.equals("DEBUG")) {
			System.out.println("Debug: MQ BackoutThreshhold =  " + iBackoutThreshHold );
			System.out.println("Debug: MQ BackoutQueue =  " + sBackoutQueue );
		}
		
		this.mqconn.OpenQueueForReading(queueName);
		this.mqconn.SetGetMessageOptions();
		byte[] message = null;
		
		// Counts ... not currently used
		int iSuccessMessagesProcessed = 0;
		int iMessagesRead = 0;
		int iInvalidMessages = 0;
		
		CreateThreadPool();		

	    // Create a SendToKafka object and pass the MessageHub object to it
		this.stk = new SendToKafka();
		this.stk.setMsgHub(this.msgHub);

		// http://www.mqseries.net/phpBB2/viewtopic.php?t=35456
		// https://www.javatips.net/api/camel-extra-master/components/camel-wmq/src/main/java/org/apacheextras/camel/component/wmq/WMQConsumer.java

		// Loop 
		//   Read messages from the MQ object
		//      if the backout count is higher than the value on the queue, write the messages
		//                       to the backout queue
		//      otherwise, post message to Kafka
		//
		while (!isStopping)
		{

			try {				
				//message = this.mqconn.GetByteMessage();
				MQMessage mqMsg = this.mqconn.GetMQMessage();
				message = new byte[mqMsg.getMessageLength()];
				mqMsg.readFully(message);
				this.met.AddProcessed();
				
				// If we have a message, process it
				if (message != null)
				{ 
					// If the message has already been processed, check the backout count and
					//   if need, push it to the backout queue
					if (this.mqconn.getBackoutCount() > iBackoutThreshHold) {
						this.met.AddFailed();

						WriteMessageToBackoutQueue(mqMsg, sBackoutQueue);
						this.mqconn.commit();
						
					} else {
						PostToKafka(message);
						this.met.AddSuccess();
					}
				}
				
			} catch (MQInvalidMessageException e) {
				
				if (e.GetCompletionCode() == 2 && 
						((e.GetReasonCode() == MQConstants.MQRC_NO_MSG_AVAILABLE)
						|| (e.GetReasonCode() == MQConstants.MQRC_GET_INHIBITED )))
				{
					System.out.println("Thread: " + currentThread().getName() );
					System.out.println("No more messages (A) - " + e.GetReasonCode() + ", " + e.getMessage());
					if (e.GetReasonCode() != MQConstants.MQRC_NO_MSG_AVAILABLE) {
					
						// Received an error other than 2033 ... so, wait for 5 seconds
						try {
							Thread.sleep(5000);
						} catch (InterruptedException e1) {
							// do nothing
						}						
					}

				} else {
					System.out.println("Thread: " + currentThread().getName() );
					System.out.println("Error: (5) - " 
							+ e.GetCompletionCode() 
							+ " ReasonCode " + e.GetReasonCode()
							+ " Description " + e.GetMessage());

					Thread.sleep(5000);
					
					if (e.GetCompletionCode() == 2 && 
							((e.GetReasonCode() == MQConstants.MQRC_CONNECTION_BROKEN))) {
						System.out.println("Info: Attempting to re-connect to the queue manager - " + this.mqp.GetQM());

						boolean isConnected = false;
						int iConnectionAttempts = 0;
						while ((!isConnected) || (iConnectionAttempts > 5)) {
							iConnectionAttempts++;
							
							try {
								CreateMQConnection();
								this.mqconn.OpenQueueForReading(rp.GetMQQueue());
								this.mqconn.SetGetMessageOptions();
								isConnected = true;
							
							} catch (Exception e1) {
								System.out.println("Info: Attempt to reconnected failed : " + iConnectionAttempts );
								Thread.sleep(5000);

							}
						}
						if (!isConnected) {
							System.out.println("Error: Failed to reconnected after 5 attempts " );
							System.exit(1);
						}
					}
				}
				
			} catch (MQException mqe) {
				if (mqe.getCompCode() == 2 && 
						mqe.getReason() == MQConstants.MQRC_NO_MSG_AVAILABLE)
				{
					System.out.println("No more messages (B) - " + mqe.getMessage());

				} else {

					System.out.println("Error: (6) - " 
							+ mqe.getCompCode() 
							+ " ReasonCode " + mqe.getReason()
							+ " Description " + mqe.getMessage());

					if (mqe.getCompCode() == 2 && 
							((mqe.getReason() == MQConstants.MQRC_CONNECTION_BROKEN))) {
						System.out.println("Info: Attempting to re-connect to the queue manager - " + this.mqp.GetQM());

						boolean isConnected = false;
						int iConnectionAttempts = 0;
						while ((!isConnected) || (iConnectionAttempts > 5)) {
							iConnectionAttempts++;
							
							try {
								CreateMQConnection();
								this.mqconn.OpenQueueForReading(rp.GetMQQueue());
								this.mqconn.SetGetMessageOptions();
								isConnected = true;
							
							} catch (Exception e1) {
								System.out.println("Info: Attempt to reconnected failed : " + iConnectionAttempts );
								Thread.sleep(5000);
							}
						}
						if (!isConnected) {
							System.out.println("Error: Failed to reconnected after 5 attempts " );
							System.exit(1);
						}
					}
				}	
			}	
		}
	}

	// Write messages to a backout queue
	private void WriteMessageToBackoutQueue(MQMessage msg, String backOutQueueName)  {

		if (showEnv.equals("DEBUG")) {
			System.out.println("Debug: Write to backoutqueue = " );		
		}

		try {
			this.mqconn.WriteToBackOutQueue(msg, backOutQueueName);

		} catch (Exception e) {
			System.out.println("Error: Error backing out message : " + e.getMessage());		
			
		}		
	}


	/*
	 *  Post messages to Kafka ...
	 */
	private void PostToKafka(byte[] message) {

		String msg = new String(message);
		if (showEnv.equals("DEBUG")) {
			System.out.println("Debug: MQ message = " + msg);		
		}
		
		// Create a callable task ...
		Callable<Integer> callableTask = () -> {			

				if (showEnv.equals("DEBUG")) {
					System.out.println("DEBUG: Task Running .... ");
				}

				// Build a MessageObject 
			    SendMsgObject sm = new SendMsgObject();
			    String xReqCorrelId = this.mqconn.getCorrelationValue();
			    if (xReqCorrelId != null) {
				    sm.msgId = xReqCorrelId;
			    } else {
			    	sm.msgId = null;			    
			    }
			    sm.payLoad = null;
			    sm.bytePayload = message;
			    sm.type = msgType.BYTES;
			    // Note: Topicname is assigned in the SendToKafka object (stk)

			    int retCode = this.stk.Send(sm);

			    // Any errors, try again
			    if (retCode != 0) {
					try {
						this.mqconn.rollback();
						
					} catch (MQException e) {
						System.out.println("Error: MQ rollback failed, continuing :  " + threadName);			
					}
					System.out.println("Warn: MQ message has been backed out : " + threadName);
				
				} else {
					if (showEnv.equals("DEBUG")) {		
						System.out.println("DEBUG: Committing : " + threadName);
					}
					try {
						this.mqconn.commit();
						  
					} catch (IOException | MQException e1) {
						System.out.println("Error: Unable to commit transaction");
					}
				}  
		
				if (showEnv.equals("DEBUG")) {
					Date dt = new Date();
					DateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.S");
					String date = dateFormat.format(dt);	
					System.out.println("DEBUG: End Timestamp for " + threadName + " : " + date);
				}
				return retCode;
		
		};

		//this.executorPool.setKeepAliveTime(5000, TimeUnit.MILLISECONDS);
		//this.executorPool.allowCoreThreadTimeOut(true);
		//Future<?> schedFuture = this.executorPool.submit(callableTask);
		
		Future<?> schedFuture = this.schedexecutorPool.submit(callableTask);
		this.schedexecutorPool.schedule(new Runnable(){
			@Override 
			public void run(){
				if (showEnv.equals("DEBUG")) {
			 		System.out.println("Info: Scheduler cancelled " + Thread.currentThread().getName());
			    	 schedFuture.cancel(true);
				}
		     }      
		 }, this.timerValue, TimeUnit.MILLISECONDS);
		
	}
	
	/*
	 * Create task thread pool
	 */
	private void CreateThreadPool() {

	    int iCorePoolSize = 0;
	    String noThreads = System.getenv("EVENTHUB_HTTP_SERVER_COREPOOL");
	    if (noThreads != null) {
	    	iCorePoolSize = Integer.parseInt(noThreads);
			if (this.showEnv.equals("DEBUG")) {
		    	System.out.println("DEBUG: EVENTHUB_HTTP_SERVER_COREPOOL set to " + iCorePoolSize );
			}
	    } else {
	    	iCorePoolSize = 5;
			if (this.showEnv.equals("DEBUG")) {
				System.out.println("DEBUG: EVENTHUB_HTTP_SERVER_COREPOOL is missing, defaulting to 5 " );
			}
	    }

	    //
		int iMaximumPoolSize = 0;
	    noThreads = System.getenv("EVENTHUB_HTTP_SERVER_MAXPOOL");
	    if (noThreads != null) {
	    	iMaximumPoolSize = Integer.parseInt(noThreads);
			if (this.showEnv.equals("DEBUG")) {
				System.out.println("DEBUG: EVENTHUB_HTTP_SERVER_MAXPOOL set to " + iMaximumPoolSize );
			}
	    } else {
	    	iMaximumPoolSize = 10;
			if (this.showEnv.equals("DEBUG")) {
				System.out.println("DEBUG: EVENTHUB_HTTP_SERVER_MAXPOOL is missing, defaulting to 10 " );
			}
	    }
	    
	    //
		long lKeepAliveTime = 0;
	    String keepAlive = System.getenv("EVENTHUB_HTTP_SERVER_KEEPALIVE");
	    if (noThreads != null) {
	    	lKeepAliveTime = Long.parseLong(keepAlive);
			if (this.showEnv.equals("DEBUG")) {
				System.out.println("DEBUG: EVENTHUB_HTTP_SERVER_KEEPALIVE set to " + lKeepAliveTime );
			}
	    } else {
	    	lKeepAliveTime = 10;
			if (this.showEnv.equals("DEBUG")) {
				System.out.println("DEBUG: EVENTHUB_HTTP_SERVER_KEEPALIVE is missing, defaulting to 10 seconds " );
			}
	    }
		
	    //
		int iArrayBlockingQueueSize = 0;
	    String blocking = System.getenv("EVENTHUB_HTTP_SERVER_BLOCKINGQUEUE");
	    if (blocking != null) {
	    	iArrayBlockingQueueSize = Integer.parseInt(blocking);
			if (this.showEnv.equals("DEBUG")) {
				System.out.println("DEBUG: EVENTHUB_HTTP_SERVER_BLOCKINGQUEUE set to " + iArrayBlockingQueueSize );
			}
	    } else {
	    	iArrayBlockingQueueSize = 10;
			if (this.showEnv.equals("DEBUG")) {
				System.out.println("DEBUG: EVENTHUB_HTTP_SERVER_BLOCKINGQUEUE is missing, defaulting to 10 " );
			}
	    }

	    if (iCorePoolSize < 0 ||
	    		iMaximumPoolSize <= 0 ||
	    		iMaximumPoolSize < iCorePoolSize ||
	    		lKeepAliveTime < 0) {
	    	throw new IllegalArgumentException("Error: Invalid ThreadPool values" 
	    			+ " CorePoolSize : " + iCorePoolSize
	    			+ " MaximumPoolSize :" + iMaximumPoolSize
	    			+ " KeepAliveSize" + lKeepAliveTime
	    			+ " BlockingQueueSize : " + iArrayBlockingQueueSize);
	    }
	    
		System.out.println("Info: Creating Threadpool  ");	
	    RejectedExecutionHandler rejectionHandler = new RejectedExecutionHandlerImp();
	    ThreadFactory threadFactory = Executors.defaultThreadFactory();

	    this.executorPool = new ThreadPoolExecutor(iCorePoolSize,iMaximumPoolSize,lKeepAliveTime,
	    		TimeUnit.SECONDS, 
	    		new ArrayBlockingQueue<Runnable>(iArrayBlockingQueueSize), 
	    		threadFactory, rejectionHandler);
	    this.schedexecutorPool = Executors.newScheduledThreadPool(iCorePoolSize); 
	    
	}
	
	
	public void StopServer() throws MQConnectionException {
		System.out.println("Info: MQ Kafka Server is stopping");
		isStopping = true;

		if (this.mqconn != null) {
			this.mqconn.Disconnect();
		}
		
		if (this.executorPool != null) {
			this.executorPool.shutdown();
		}
		if (this.schedexecutorPool != null) {
			this.schedexecutorPool.shutdown();
		}

		if (this.msgHub != null) {
			this.msgHub.CloseMessageHub();
		}
		
		shutdown = true;
		
	}

	// Load the MQ properties
	private void getMQProperties() {
		
		try {
			rp = new ResourceProperties("resources\\MQConfig.properties");
		
		} catch (InvalidResourcePropertiesException e) {
			logger.log(Level.FATAL, "Error loading resource file ...");
			logger.log(Level.FATAL, "Error - ReasonCode: " 
						+ e.GetReasonCode() + " Desc: " + e.GetMessage());
			System.out.println("MQKafkaProcess : Reason: " + e.GetReasonCode() + " Desc:" + e.GetMessage());
			e.printStackTrace();

		}

		this.mqp = new MQParameters();
		this.mqp.SetHost(rp.GetMQHost());
		this.mqp.SetQM(rp.GetMQQueueManager());
		this.mqp.SetChannel(rp.GetMQChannel());
		this.mqp.SetPort(rp.GetMQPort());
		this.mqp.SetUserId(rp.GetMQUserid());
		this.mqp.SetPassword(rp.GetMQPassword());

		String ccdtDir = rp.GetCCDTDirectory();
		String ccdtFile = rp.GetCCDTFile();
		
		if ((ccdtDir.equals("")) || (ccdtFile.equals(""))) {
			this.mqp.SetUsingCCDT(false);
		} else {
			this.mqp.SetUsingCCDT(true);
			this.mqp.SetCCDTDirectory(rp.GetCCDTDirectory());
			this.mqp.SetCCDTFile(rp.GetCCDTFile());
		}

		this.mqp.SetTrustStore(rp.GetTrustStore());
		this.mqp.SetKeyStore(rp.GetKeyStore());
		this.mqp.SetKeyPass(rp.GetKeyPassword());
		this.mqp.SetCipherSuite(rp.GetCipherSuite());
		
		String sslChannel = rp.GetUseSSLCahnnel();
		if (!(sslChannel.equals("true"))) {
			this.mqp.SetUseSSLChannel(false);
		} else {
			this.mqp.SetUseSSLChannel(true);
		}
		
	}

	// Get the environment variables
	private void getEnvironmentProperties() {

		try
		{
			showEnv = System.getenv("EVENTHUB_TRACE");
		} catch (Exception localException1) {
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

		// timer
		try {
			String timerVal = System.getenv("EVENTHUB_HTTP_SERVER_TIMEOUT");
			timerValue = Integer.parseInt(timerVal);
		}
		catch (Exception e) {
			timerValue = 5000;
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


