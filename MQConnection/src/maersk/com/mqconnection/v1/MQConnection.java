package maersk.com.mqconnection.v1;

import java.io.DataInput;
import java.io.DataInputStream;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Enumeration;
import java.util.Hashtable;
import java.util.List;
import java.util.Properties;

import javax.net.ssl.SSLContext;

import com.ibm.mq.*;
import com.ibm.mq.constants.CMQC;
import com.ibm.mq.constants.MQConstants;
import com.ibm.mq.headers.MQDLH;
import com.ibm.mq.headers.MQDataException;
import com.ibm.mq.headers.MQHeader;
import com.ibm.mq.headers.MQHeaderIterator;
import com.ibm.mq.headers.MQHeaderList;
import com.ibm.mq.headers.MQRFH2;

import maersk.com.mqparameters.v1.*;

public class MQConnection {

	private boolean bAlreadyOpen = false;

	protected String queueManager;
	
	private MQQueueManager qmgr;
	private MQQueue inQueue;
	private MQMessage message;
	
	private int iBackoutCount;
	private int iBackoutThreshHoldCount;
	private String sBackoutQueue;
	
	//private MQEnvironment env;
	
	private MQMessage inMessage;
	private MQGetMessageOptions gmo;
	
	private MQQueue outQueue;
	private MQMessage respMessage;
	private MQPutMessageOptions pmo;
	
	private ArrayList<String> responseQueues;
	
	private String CorrelationValue;
	private String DLQ;

	private MQParameters params;
	
	private boolean useCCDT;
	public boolean isUseCCDT() {
		return useCCDT;
	}
	public void setUseCCDT(boolean useCCDT) {
		this.useCCDT = useCCDT;
	}

	private boolean useSSL;
	public boolean useSSL() {
		return useSSL;
	}
	public void setUseSSL(boolean useSSL) {
		this.useSSL = useSSL;
	}

	
	// Default constructor
	public MQConnection() throws MQConnectionException
	{			
		this.responseQueues = new ArrayList<String>();
	}

	// Constructor, passing in MQParameters object
	public MQConnection(MQParameters params) throws MQConnectionException
	{
		OpenLocalQueueManager(params);		
	}

	private void OpenLocalQueueManager(MQParameters params) throws MQConnectionException {

		this.params = params;
		int openOptions = CMQC.MQOO_FAIL_IF_QUIESCING 
				+ CMQC.MQCNO_RECONNECT;

		try
		{
			this.qmgr = new MQQueueManager(this.queueManager, openOptions);
			this.DLQ = this.qmgr.getAttributeString(CMQC.MQCA_DEAD_LETTER_Q_NAME, 48);
		}
		catch (MQException mqe)
		{
			throw new MQConnectionException(mqe.completionCode, mqe.reasonCode, mqe.getMessage());
		}

	}

	// SetConnectionParameters object
	public void SetConnectionParameters(MQParameters params) throws MQConnectionException
	{
		this.params = params;		
		this.queueManager = this.params.GetQM();
		MQEnvironment.hostname = this.params.GetHost();
		MQEnvironment.channel = this.params.GetChannel();
		MQEnvironment.port = this.params.GetPort();
		MQEnvironment.userID = this.params.GetUserId();
		MQEnvironment.password = this.params.GetPassword();

	}

	//
	public void CreateConnection() throws MQConnectionException, MalformedURLException {
		
		if (!this.useCCDT) {
			CreateConnectionUsingEnvionrment();
			
		} else {
			CreateConnectionUsingCCDT();
		}
	}
	
	private void CreateConnectionUsingEnvionrment() throws MQConnectionException {
		
		if (this.useSSL) {
			CreateMQConnectionUsingSSL();
		
		} else {
			CreateMQConnection();
		}
	}

	//
	// Create Queue Manager object using environment parameters already set
	//
	private void CreateMQConnectionUsingSSL() throws MQConnectionException
	{
		System.out.println("Info: Connecting using SSL channel " + this.params.GetChannel());

		MQEnvironment.userID = null;
		MQEnvironment.password = null;
		//
		MQEnvironment.sslCipherSuite = this.params.GetCipherSuite();
        MQEnvironment.sslFipsRequired = false;
		
        System.setProperty("javax.net.ssl.trustStore", this.params.GetTrustStore());
        System.setProperty("javax.net.ssl.trustStoreType","JKS");
        System.setProperty("javax.net.ssl.keyStore",this.params.GetKeyStore());
        System.setProperty("javax.net.ssl.keyStorePassword",this.params.GetKeyPass());
        System.setProperty("javax.net.ssl.keyStoreType","JKS");
        System.setProperty("com.ibm.mq.cfg.useIBMCipherMappings","false");

		int openOptions = CMQC.MQOO_FAIL_IF_QUIESCING 
				+ CMQC.MQCNO_RECONNECT; 
		//		+ CMQC.MQOO_INPUT_SHARED;
		
		try
		{
			this.qmgr = new MQQueueManager(this.queueManager, openOptions);
			this.DLQ = this.qmgr.getAttributeString(CMQC.MQCA_DEAD_LETTER_Q_NAME, 48).trim();
			System.out.println("Info: Connected to MQ Server");
			System.out.println("Info: DLQ defined as " + this.DLQ);
		}
		catch (MQException mqe)
		{
			mqe.printStackTrace();
			throw new MQConnectionException(mqe.completionCode, mqe.reasonCode, mqe.getMessage());
		}
		
	}
	
	//
	// Create Queue Manager object using environment parameters already set
	//
	private void CreateMQConnection() throws MQConnectionException
	{
		System.out.println("Info: Connecting using NONE SSL channel " + this.params.GetChannel());
		
		int openOptions = CMQC.MQOO_FAIL_IF_QUIESCING 
				+ CMQC.MQCNO_RECONNECT; 
		
		try
		{
			this.qmgr = new MQQueueManager(this.queueManager, openOptions);
			this.DLQ = this.qmgr.getAttributeString(CMQC.MQCA_DEAD_LETTER_Q_NAME, 48).trim();
			System.out.println("Info: Connected to MQ Server");
			System.out.println("Info: DLQ defined as " + this.DLQ);

		}
		catch (MQException mqe)
		{
			throw new MQConnectionException(mqe.completionCode, mqe.reasonCode, mqe.getMessage());
		}
		
	}
	
	private void CreateConnectionUsingCCDT() throws MalformedURLException, MQConnectionException {
		
		if (this.useSSL) {
			CreateConnectionCCDTSSL();
			
		} else {
			CreateConnectionCCDT();
		}
	}
	
	// Create an SSL connection from the CCDT file
	private void CreateConnectionCCDTSSL() throws MQConnectionException, MalformedURLException {
		
		System.out.println("Info: Connecting using CCDT TLS");
		
		//-Djavax.net.ssl.trustStore=C:/Users/mickm/Documents/Development/MQSSL03/kafkakeystore.jks 
		//-Djavax.net.ssl.keyStore=C:/Users/mickm/Documents/Development/MQSSL03/kafkakeystore.jks
				
		//String qmName = "*TSTQPD01GWS";
		String qmName = this.params.GetQM();
		System.out.println("Info: Connecting to " + qmName);
		
		String fileName = this.params.GetCCDTDirectory() + this.params.GetCCDTFile();
		URL ccdtFile = new URL("file:///" + fileName);

        System.setProperty("javax.net.ssl.trustStore", this.params.GetTrustStore());
        System.setProperty("javax.net.ssl.trustStoreType","JKS");
        System.setProperty("javax.net.ssl.keyStore",this.params.GetKeyStore());
        System.setProperty("javax.net.ssl.keyStorePassword",this.params.GetKeyPass());
        System.setProperty("javax.net.ssl.keyStoreType","JKS");
        System.setProperty("com.ibm.mq.cfg.useIBMCipherMappings","false");

		int openOptions = CMQC.MQOO_FAIL_IF_QUIESCING 
				+ CMQC.MQCNO_RECONNECT; 
		//		+ CMQC.MQOO_INPUT_SHARED;

		
	 	Hashtable<String, String> props = new Hashtable<String, String>();
		props.put(CMQC.TRANSPORT_PROPERTY,CMQC.TRANSPORT_MQSERIES_CLIENT);
		
		// Create a connection and get the DLQ
		try
		{
			this.qmgr = new MQQueueManager(qmName, props, ccdtFile);

		//	this.qmgr = new MQQueueManager(qmName, props, ccdtFile);
			this.DLQ = this.qmgr.getAttributeString(CMQC.MQCA_DEAD_LETTER_Q_NAME, 48).trim();
			System.out.println("Info: Connected to MQ Server");
			System.out.println("Info: DLQ defined as " + this.DLQ);

		}
		catch (MQException mqe)
		{
			mqe.printStackTrace();
			throw new MQConnectionException(mqe.completionCode, mqe.reasonCode, mqe.getMessage());
		}
		
	}
	
	// Create an SSL connection from the CCDT file
	private void CreateConnectionCCDT() throws MQConnectionException, MalformedURLException {

		System.out.println("Info: Connecting using CCDT");
		String qmName = this.params.GetQM();
		System.out.println("Info: Connecting to " + qmName);
		
		String fileName = this.params.GetCCDTDirectory() + this.params.GetCCDTFile();
		URL ccdtFile = new URL("file:///" + fileName);

		int openOptions = CMQC.MQOO_FAIL_IF_QUIESCING 
				+ CMQC.MQCNO_RECONNECT; 
		
	 	Hashtable<String, String> props = new Hashtable<String, String>();
		props.put(CMQC.TRANSPORT_PROPERTY,CMQC.TRANSPORT_MQSERIES_CLIENT);
		
		// Create a connection and get the DLQ
		try
		{
			this.qmgr = new MQQueueManager(qmName, props, ccdtFile);
			this.DLQ = this.qmgr.getAttributeString(CMQC.MQCA_DEAD_LETTER_Q_NAME, 48).trim();
			System.out.println("Info: Connected to MQ Server");
			System.out.println("Info: DLQ defined as " + this.DLQ);

		}
		catch (MQException mqe)
		{
			mqe.printStackTrace();
			throw new MQConnectionException(mqe.completionCode, mqe.reasonCode, mqe.getMessage());
		}
		
	
	}
	

	// Open the queue for input ...
	public void OpenQueueForReading(String queueName) throws MQInvalidMessageException {
		
		//String resp = null;
		this.inQueue = null;
		int openOptions = CMQC.MQOO_FAIL_IF_QUIESCING 
				+ CMQC.MQOO_INQUIRE 
				+ CMQC.MQOO_INPUT_SHARED;
		
		try {
			this.inQueue = this.qmgr.accessQueue(queueName, openOptions);
			int iDepth = this.inQueue.getCurrentDepth();
			System.out.println("Info: Current depth: " + iDepth);
			
		} catch (MQException e) {
			throw new MQInvalidMessageException(e.completionCode, e.reasonCode, "Unable to connect to queue " + queueName);			
		}
		
	}

	// Set Message Options
	public void SetGetMessageOptions() {
		
		this.gmo = new MQGetMessageOptions();
		this.gmo.options = CMQC.MQGMO_WAIT 
				+ CMQC.MQGMO_FAIL_IF_QUIESCING 
				+ CMQC.MQGMO_CONVERT
				+ CMQC.MQGMO_SYNCPOINT
				+ CMQC.MQGMO_PROPERTIES_IN_HANDLE;
		
		// wait 5 seconds
		this.gmo.waitInterval = 5000;
		
	}

	// Read the message from the queue ....
	public String GetMessage() throws MQInvalidMessageException, MQException {
		
		String mess = null;
		this.inMessage = new MQMessage();
		try {
			
			this.inQueue.get(this.inMessage, this.gmo);
			byte[] b = new byte[this.inMessage.getMessageLength()];
			this.inMessage.readFully(b);
			mess = new String(b);
			
		}
		catch (IOException e)
		{
			try {
				this.qmgr.backout();
				
			} catch (MQException e1) {
				throw new MQInvalidMessageException(e1.completionCode,e1.reasonCode,e1.getMessage());
			
			}
		}
		catch (MQException e)
		{
			throw new MQInvalidMessageException(e.completionCode, e.reasonCode, e.getMessage());		
			
		} catch (Exception e) {
			throw new MQInvalidMessageException(e.getMessage());		
		}
		
		return mess;
		
		
	}

	// Get an MQMessage object
	public MQMessage GetMQMessage() throws MQInvalidMessageException, MQException {
		
		byte[] byteMsg = null;
		MQMessage msg = new MQMessage();
		try {


			this.inQueue.get(msg, this.gmo);
			setBackoutCount(msg.backoutCount);			
			setCorrelationValue(null);
			
		    Enumeration props = msg.getPropertyNames("X-MESSAGE-CORRELATION-ID"); 
		    try {
		    	
			    if (props != null) { 
			          String propName = (String) props.nextElement(); 
			          Object propObject = msg.getObjectProperty(propName); 
			          String value = (String) propObject;
			          setCorrelationValue(value);
			    }
		    } catch (Exception e) {
			    System.out.println("Info: Correlation field X-MESSAGE-CORRELATION-ID is missing"); 		    	
		    }
		    
		}
		catch (MQException e){			
			this.qmgr.backout();
			throw new MQInvalidMessageException(e.completionCode, e.reasonCode, e.getMessage());		
			
		} catch (Exception e) {
			throw new MQInvalidMessageException(e.getMessage());		
		}
		
		return msg;
		
	}

	// Read the message from the queue ....
	public byte[] GetByteMessage() throws MQInvalidMessageException, MQException {
		
		byte[] byteMsg = null;
		this.inMessage = new MQMessage();
		try {

			this.inQueue.get(this.inMessage, this.gmo);
			byteMsg = new byte[this.inMessage.getMessageLength()];
			this.inMessage.readFully(byteMsg);			
			setBackoutCount(this.inMessage.backoutCount);			
			setCorrelationValue(null);
			
		    Enumeration props = this.inMessage.getPropertyNames("X-MESSAGE-CORRELATION-ID"); 
		    if (props != null) { 
		          String propName = (String) props.nextElement(); 
		          Object propObject = this.inMessage.getObjectProperty(propName); 
		          String value = (String) propObject;
		          setCorrelationValue(value);
		    }
		    	
		}
		catch (IOException e)
		{
			try {
				this.qmgr.backout();
				
			} catch (MQException e1) {
				throw new MQInvalidMessageException(e1.completionCode,e1.reasonCode,e1.getMessage());
			
			}
		}
		catch (MQException e)
		{
			throw new MQInvalidMessageException(e.completionCode, e.reasonCode, e.getMessage());		
			
		} catch (Exception e) {
			throw new MQInvalidMessageException(e.getMessage());		
		}
		
		return byteMsg;
		
		
	}

	// Open the queue for input ...
	public MQQueue OpenQueueForWriting(String queueName) throws MQInvalidMessageException {

		//this.outQueue = null;
		MQQueue outQueue = null;
		
		int openOptions = CMQC.MQOO_FAIL_IF_QUIESCING 
				+ CMQC.MQOO_INPUT_AS_Q_DEF 
				+ CMQC.MQOO_OUTPUT ;

	//	int openOptions = CMQC.MQOO_FAIL_IF_QUIESCING 
	//			+ CMQC.MQOO_OUTPUT 
	//			+ CMQC.MQOO_INPUT_SHARED;
		
		try {
			outQueue = this.qmgr.accessQueue(queueName, openOptions);
		//	int iDepth = this.inQueue.getCurrentDepth();
		//	System.out.println("Current depth: " + iDepth);
			
		} catch (MQException e) {
			throw new MQInvalidMessageException(e.completionCode, e.reasonCode, "Unable to open queue " 
						+ queueName);			
		}
		
		return outQueue;
		
	}

	// Open the queue for input ...
	public MQQueue OpenQueueForWriting1(String queueName) throws MQInvalidMessageException {

		MQQueue queue = null;
		int openOptions = CMQC.MQOO_FAIL_IF_QUIESCING 
				+ CMQC.MQOO_OUTPUT ;

	//	int openOptions = CMQC.MQOO_FAIL_IF_QUIESCING 
	//			+ CMQC.MQOO_OUTPUT 
	//			+ CMQC.MQOO_INPUT_SHARED;
		
		try {
			queue = this.qmgr.accessQueue(queueName, openOptions);
		//	int iDepth = this.inQueue.getCurrentDepth();
		//	System.out.println("Current depth: " + iDepth);
			
		} catch (MQException e) {
			throw new MQInvalidMessageException(e.completionCode, e.reasonCode, "Unable to open queue " 
						+ queueName);			
		}
		
		return queue;
	}

	public void WriteToQueue(String message, MQQueue queue) throws MQInvalidMessageException, MQException, IOException {
		
		MQMessage newmsg = new MQMessage();

	    newmsg.format  					= CMQC.MQFMT_STRING;
	    newmsg.feedback               	= CMQC.MQFB_NONE;
	    newmsg.messageType            	= CMQC.MQMT_DATAGRAM;

	    newmsg.write(message.getBytes());
	    
		this.pmo = new MQPutMessageOptions();
		MQPutMessageOptions pmo = new MQPutMessageOptions();
		pmo.options = CMQC.MQPMO_FAIL_IF_QUIESCING 
				+ CMQC.MQPMO_NEW_MSG_ID
				+ CMQC.MQPMO_SYNCPOINT;
		
		
		queue.put(newmsg, pmo); 

	}

	public void WriteToQueue(MQMessage msg, MQQueue queue) throws MQInvalidMessageException, MQException, IOException {
		
		this.pmo = new MQPutMessageOptions();
		MQPutMessageOptions pmo = new MQPutMessageOptions();
		pmo.options = CMQC.MQPMO_FAIL_IF_QUIESCING 
		//		+ CMQC.MQRO_PASS_MSG_ID
				+ CMQC.MQPMO_NEW_MSG_ID
				+ CMQC.MQPMO_SYNCPOINT;
		
		
		queue.put(msg, pmo); 

	}

	// Send the message back to the reply queue ....
	public void SendResponse(String message) throws MQInvalidMessageException, MQException, IOException {

		if (this.inMessage.messageType != CMQC.MQMT_REQUEST) {
			throw new MQInvalidMessageException(3001,2,"Invalid message type");
		}
		
		this.respMessage = new MQMessage();
		if (!this.responseQueues.contains(this.inMessage.replyToQueueName)) {
			this.responseQueues.clear();
			this.outQueue = OpenQueueForWriting(this.inMessage.replyToQueueName);
			this.responseQueues.add(this.inMessage.replyToQueueName);
		}
		
	    this.respMessage.format  					= CMQC.MQFMT_STRING;
	    this.respMessage.feedback               	= CMQC.MQFB_NONE;
	    this.respMessage.messageType            	= CMQC.MQMT_REPLY;
	    
	    this.respMessage.correlationId				= this.inMessage.messageId;
	    this.respMessage.replyToQueueName       	= this.inMessage.replyToQueueName;
	    this.respMessage.replyToQueueManagerName	= this.inMessage.replyToQueueManagerName;
	    
	    
	    this.respMessage.write(message.getBytes());
	    
		this.pmo = new MQPutMessageOptions();
		MQPutMessageOptions pmo = new MQPutMessageOptions();
		pmo.options = CMQC.MQPMO_FAIL_IF_QUIESCING 
				+ CMQC.MQPMO_NEW_MSG_ID
				+ CMQC.MQPMO_SYNCPOINT;
		
		
		//this.outQueue.put(this.respMessage, pmo); 
		this.outQueue.put(this.respMessage, pmo); 

	    
	}
	
	public synchronized void commit() throws IOException, MQException {
		
		//if (this.inMessage.backoutCount > 2) {
		//	System.out.println("Threashold");
		//}
		
		if (this.inMessage != null) {
			this.inMessage.clearMessage();			
		}
		this.qmgr.commit();
		
	}

	public synchronized void rollback() throws MQException {
		
		this.qmgr.backout();
		
	}

	// Disconnect from queue manager
	public void Disconnect() throws MQConnectionException
	{
		try {
			this.inQueue.close();
			this.outQueue.close();			
		}
		catch (Exception e)
		{
			// do nothing 
		}
				
		try
		{
			this.qmgr.disconnect();
		}
		catch (MQException mqe)
		{
			throw new MQConnectionException(mqe.completionCode, mqe.reasonCode, mqe.getCause());
		}

	}

	public synchronized void InquireQueue(int[] query, String queueName, int[] outi, byte[] outb) 
				throws MQInvalidMessageException, MQException {
	
		MQQueue queue = null;
		int openOptions = CMQC.MQOO_FAIL_IF_QUIESCING 
				+ CMQC.MQOO_INQUIRE 
				+ CMQC.MQOO_INPUT_SHARED;

	//	int openOptions = CMQC.MQOO_FAIL_IF_QUIESCING 
	//			+ CMQC.MQOO_OUTPUT 
	//			+ CMQC.MQOO_INPUT_SHARED;
		
		try {
			queue = this.qmgr.accessQueue(queueName, openOptions);
			queue.inquire(query, outi, outb);
			
		} catch (MQException e) {
			System.out.println("INFO: Unable to inquire on queue " + queueName);
			//throw new MQInvalidMessageException(e.completionCode, e.reasonCode, "Unable to inquire on queue " 
			//			+ queueName);			
		}

		
		int backOutThreshHold = 0;
		if (queue != null) {
			backOutThreshHold = outi[0];
		}
		String bq = new String(outb).trim();
		
		setBackoutThreshHoldCount(backOutThreshHold);
		setBackoutThreshHoldQueue(bq);
		queue.close();
	}

	public synchronized int getBackoutCount() {
		return iBackoutCount;
	}

	private void setBackoutCount(int iBackoutCount) {
		this.iBackoutCount = iBackoutCount;
	}

	public synchronized int getBackoutThreshHoldCount() {
		return iBackoutThreshHoldCount;
	}

	private void setBackoutThreshHoldCount(int iBackoutThreshHoldCount) {
		this.iBackoutThreshHoldCount = iBackoutThreshHoldCount;
	}

	public synchronized String getBackoutThreshHoldQueue() {
		return sBackoutQueue;
	}

	private void setBackoutThreshHoldQueue(String sBackoutQueue) {
		this.sBackoutQueue = sBackoutQueue;
	}

	public synchronized String getCorrelationValue() {
		return CorrelationValue;
	}
	private void setCorrelationValue(String correlationValue) {
		CorrelationValue = correlationValue;
	}

	/*
	 * Write message to backout queue ... if there is one .. if not, try the DLQ
	 */
	public synchronized void WriteToBackOutQueue(MQMessage msg, String backOutQueueName) 
			throws MQInvalidMessageException, MQException, MQDataException {

		backOutQueueName = "DOESNOTEXIST";
		MQQueue queue = null;
		try {
			queue = OpenQueueForWriting(backOutQueueName); // Was OpenQueueForWriting1
			WriteToQueue(msg, queue);
			
		} catch (Exception e) {
			WriteToDLQ(msg);
						
		} finally {
			if (queue != null) {
				queue.close();
			}
		}
	}
	
	public synchronized void WriteToDLQ(MQMessage msg) throws MQInvalidMessageException, MQDataException {
		
		MQQueue DLQqueue = null;
		try {
			DLQqueue = OpenQueueForWriting(this.DLQ); //Was OpenQueueForWriting1		
			WriteToQueue(msg, DLQqueue);
			
		} catch (MQException e) {
			System.out.println("Fatal: MQException Error writting to DLQ " + this.DLQ);
			System.out.println("Fatal: Reason : " + e.reasonCode + " Desc: " + e.getMessage());
			
						
		} catch (IOException e) {
			System.out.println("Fatal: IOException Error writting to DLQ " + this.DLQ);
			System.out.println("Fatal: Desc: " + e.getMessage());

		} finally {
			try {
				if (DLQqueue != null) {
					DLQqueue.close();
				}	
				
			} catch (MQException e) {
				System.out.println("Fatal: MQException Error Closing DLQ " + this.DLQ);
				System.out.println("Fatal: Reason : " + e.reasonCode + " Desc: " + e.getMessage());
			}
		}
		
	}
	
}
