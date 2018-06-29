package maersk.com.mqjmsconnector.v1;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.lang.Thread.UncaughtExceptionHandler;
import java.security.InvalidParameterException;
import java.security.KeyManagementException;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.UnrecoverableKeyException;
import java.security.cert.CertificateException;

import javax.jms.Connection;
import javax.jms.Destination;
import javax.jms.JMSConnectionFactory;
import javax.jms.JMSContext;
import javax.jms.JMSException;
import javax.jms.MessageConsumer;
import javax.jms.MessageListener;
import javax.jms.MessageProducer;
import javax.jms.Session;
import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSocketFactory;
import javax.net.ssl.TrustManagerFactory;

import com.ibm.mq.jms.JMSC;
import com.ibm.mq.jms.MQConnection;
import com.ibm.mq.jms.MQConnectionFactory;

//import org.apache.log4j.Level;

import com.ibm.msg.client.jms.JmsConnectionFactory;
import com.ibm.msg.client.jms.JmsFactoryFactory;
import com.ibm.msg.client.wmq.WMQConstants;

//import maersk.com.mqconnection.v1.MQConnectionException;
import maersk.ibm.message.hub.v1.MessageHub;

import maersk.ibm.message.hub.v1.MessageHub;
//import maersk.mq.kafka.send.SendToKafka;

public class MQJMSConnector {
	
    private Connection connection = null;
    private Session session = null;
    private Destination destination = null;
    private Destination tempDestination = null;
    private MessageProducer producer = null;
    private MessageConsumer consumer = null;

    private JmsFactoryFactory ff = null;
   // private JmsConnectionFactory cf = null;
    private MQConnectionFactory cf = null;
    private MQConnection mqconn = null;
    		
	private MessageHub msgHub = null;

	public MQJMSConnector() throws JMSException {
		
		
		
		Thread.setDefaultUncaughtExceptionHandler(new UncaughtExceptionHandler() {		
			@Override
			public void uncaughtException(Thread t, Throwable e) {
				System.out.print("ERROR: UncaughtException Handler in MQKafkaServer\n");

				synchronized (msgHub) {
					
					msgHub.notify();
					//
				//	logger.log(Level.INFO, "Info: MQ Kafka Server stopping");
					if (msgHub != null) {
						try {
							msgHub.CloseMessageHub();
							
						} catch (Exception e1) {
							System.out.println("Error: Error closing MessageHub connection");
							
						}
					}
					//
					try {
						CloseMQConnection();
						
					} catch (JMSException e1) {
						System.out.println("Error: Error closing MQ connection");
					}
					
					//
					System.out.println("Info: MQ Kafka Server stopped");
				
				}

			//	System.exit(1);
			}
		});
		
		CreateJMSFactory();
		
	}
	
	public void CreateJMSFactory() throws JMSException {

		try {
			Class.forName("com.sun.net.ssl.internal.ssl.Provider");
		
		} catch (ClassNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			System.exit(1);
		}
		
		this.ff = JmsFactoryFactory.getInstance(WMQConstants.WMQ_PROVIDER);
	//	this.cf = this.ff.createConnectionFactory();
		this.cf = new MQConnectionFactory();
	//	JMSContext ctx = this.cf.createContext();
		
		// https://www.ibm.com/developerworks/websphere/library/techarticles/0510_fehners/0510_fehners.html
		
			
	}
	
	public void setJMSConnectionProperties() throws JMSException, KeyStoreException, NoSuchAlgorithmException, CertificateException, IOException, KeyManagementException, UnrecoverableKeyException {

		// https://www.ibm.com/support/knowledgecenter/en/SSFKSJ_8.0.0/com.ibm.mq.tro.doc/q123400_.htm
		// https://www.ibm.com/developerworks/community/blogs/aimsupport/entry/mq_amq9637_channel_is_lacking_a_certificate?lang=en
		
		//-Djava.library.path="C:\Program Files\IBM\WebSphere MQ\java\lib64"
		//		-Djavax.net.ssl.trustStore="C:/Users/mickm/Documents/Development/MQSSL03/kafkakeystore.jks" 
		//		-Djavax.net.ssl.keyStore="C:/Users/mickm/Documents/Development/MQSSL03/kafkakeystore.jks"
		//		-Djavax.net.ssl.keyStorePassword=passw0rd	
		
	//	CreateSSLContext();
		
        System.out.println("Info: Creating Connection Factory");
	//	this.cf = this.ff.createConnectionFactory();

        
	//	this.cf = this.ff.createConnectionFactory();
			
        // Set MQ properties
        this.cf.setStringProperty(WMQConstants.WMQ_QUEUE_MANAGER, "TSTQPD01");
        this.cf.setIntProperty(WMQConstants.WMQ_CONNECTION_MODE, WMQConstants.WMQ_CM_CLIENT);

        this.cf.setIntProperty(WMQConstants.WMQ_PORT,1414);
        this.cf.setStringProperty(WMQConstants.WMQ_HOST_NAME, "localhost");
        this.cf.setStringProperty(WMQConstants.WMQ_CHANNEL, "KAFKA.SVRCONN_SSL");
   //     this.cf.setTransportType(JMSC.MQJMS_TP_CLIENT_MQ_TCPIP);
   //     this.cf.setTransportType(WMQConstants.WMQ_CLIENT_JMS_COMPLIANT);
        
        this.cf.setStringProperty(WMQConstants.USERID, "KAFKA01");
        this.cf.setStringProperty(WMQConstants.PASSWORD, "ThisIsKafka");

   //     this.cf.setSSLCertStores("C:/Users/mickm/Documents/Development/MQSSL03/kafkakeystore.jks");
        
        this.cf.setSSLFipsRequired(false);
        this.cf.setSSLCipherSuite("TLS_RSA_WITH_AES_256_CBC_SHA256");
     
        //   this.cf.setSSLCipherSuite("SSL_RSA_WITH_AES_256_CBC_SHA");

        
	}

	// Work around for SSL/TLS issue with JMS
	private void CreateSSLContext() throws KeyStoreException, NoSuchAlgorithmException, CertificateException, IOException, KeyManagementException, JMSException, UnrecoverableKeyException {
		
		/*
		-Djava.library.path="C:\Program Files\IBM\WebSphere MQ\java\lib64"
				-Djavax.net.ssl.trustStore="C:/Users/mickm/Documents/Development/MQSSL03/kafkakeystore.jks" 
				-Djavax.net.ssl.keyStore="C:/Users/mickm/Documents/Development/MQSSL03/kafkakeystore.jks"
				-Djavax.net.ssl.keyStorePassword=passw0rd
			*/
		String keystoreFileName = "C:/Users/mickm/Documents/Development/MQSSL03/kafkakeystore.jks";
		String keypass = "passw0rd";
		char[] keystorePass = keypass.toCharArray();
		
		KeyStore keyStore = KeyStore.getInstance("JKS");
		FileInputStream keyStoreInputStream = new FileInputStream(keystoreFileName);
		keyStore.load (keyStoreInputStream, keystorePass);
		
		KeyStore trustStore = KeyStore.getInstance("JKS");
		FileInputStream trustStoreInputStream = new FileInputStream(keystoreFileName);
		keyStore.load (trustStoreInputStream, keystorePass);
		
		keyStoreInputStream.close();
		trustStoreInputStream.close();
		
		KeyManagerFactory keyManagerFactory 
			= KeyManagerFactory.getInstance(KeyManagerFactory.getDefaultAlgorithm());
		TrustManagerFactory trustManagerFactory 
			= TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm());
		
		keyManagerFactory.init(keyStore, keystorePass);
		trustManagerFactory.init(trustStore);
		
		SSLContext sslContext = SSLContext.getInstance("TLSv1.2");
		sslContext.init(keyManagerFactory.getKeyManagers(), 
				trustManagerFactory.getTrustManagers(), 
				null);
		
		SSLSocketFactory sslSocketFactory = 
				sslContext.getSocketFactory();
		
		// JMS
		this.cf.setObjectProperty(WMQConstants.WMQ_SSL_SOCKET_FACTORY, sslSocketFactory);
		
		
	}

	private void setSSLCipherSuite(String string) {
		// TODO Auto-generated method stub
		
	}

	public void createConnection() throws JMSException, InterruptedException {
		
	    System.out.println("Info: JMS Creating connection");

		this.connection = this.cf.createConnection();
	    System.out.println("Info: JMS Connection created.");

	    this.session = connection.createSession(true, Session.SESSION_TRANSACTED); 
	    		//Session.CLIENT_ACKNOWLEDGE);
        System.out.println("Info: JMS Session created.");

        this.destination = session.createQueue("KAFKA.TEST.REQ");
        System.out.println("Info: JMS Destination created: " + destination.toString());
        
        this.consumer = session.createConsumer(destination);
        System.out.println("Info: JMS Consumer created.");

        System.out.println("Info: Starting MQ JMS Listener");
        JMSMessageListener jmsListener = new JMSMessageListener();
        jmsListener.setMessageHub(msgHub);
        jmsListener.setSession(session);
        
    	this.consumer.setMessageListener(jmsListener);
        System.out.println("Info: Listener running");

	    try {
			connection.start();
            System.out.println("Info: Connection successfully started");            
			
		} catch (JMSException e) {
			e.printStackTrace();
			System.exit(1);
		}

	   // Thread.sleep(60000);
	    
	    waitforit();
	    
	}

	private void waitforit() throws InterruptedException {
        System.out.println("Info: Processing messages");
        
		synchronized (this.msgHub) {

			this.msgHub.wait();
		}		
	}
	

	public void setKafkaConnection() throws InvalidParameterException, Exception {

		System.out.println("Info: Creating MessageHub (Kafka) connection " );
		if (this.msgHub == null) {
			this.msgHub = new MessageHub();
		}
		System.out.println("Info: MessageHub connection created " );

	}
	
	private void CloseMQConnection() throws JMSException {
		
		if (connection != null) {
			System.out.println("Closing MQ connection");
			session.close();
			System.out.println("session closed");
			
			connection.close();
			System.out.println("connection closed");
		}
		
		
	}
	
}

