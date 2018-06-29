package maersk.com.resourceproperties.v1;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.InvalidPropertiesFormatException;
import java.util.Properties;

public class ResourceProperties {

	private String databaseConnectionString;
	private String databaseTimeOut;
	private String databaseUserId;
	private String databasePassword;
	
	private String mqHost;
	private String mqQueueManager;
	private String mqChannel;
	private int mqPort;
	private String mqUserId;
	private String mqPassword;
	private String mqQueueName;
	private String mqCCDTDirectory;
	private String mqCCDTFile;
	private String mqUseSSLChannel;
	
	private String keyStoreFilename;
	private String keyStorePass;
	private String trustStoreFilename;
	private String sslCipherSuite;
	
	public String GetDatabaseConnection() { return this.databaseConnectionString; }
	public String GetDatabaseTimeout() { return this.databaseTimeOut; }
	public String GetDatabaseUserId() { return this.databaseUserId; }
	public String GetDatabasePassword() { return this.databasePassword; }
	//
	public String GetMQHost() { return this.mqHost; }
	public String GetMQQueueManager() { return this.mqQueueManager; }
	public String GetMQChannel() { return this.mqChannel; }
	public int GetMQPort() { return this.mqPort; }
	public String GetMQUserid() { return this.mqUserId; }
	public String GetMQPassword() { return this.mqPassword; }

	public String GetMQQueue() { return this.mqQueueName; }
	
	public String GetCCDTDirectory() { return this.mqCCDTDirectory; }
	public String GetCCDTFile() { return this.mqCCDTFile; }
	
	public String GetUseSSLCahnnel() { return this.mqUseSSLChannel; }
	
	public String GetKeyStore() { return this.keyStoreFilename; }
	public String GetTrustStore() { return this.trustStoreFilename; }
	public String GetKeyPassword() { return this.keyStorePass; }
	public String GetCipherSuite() { return this.sslCipherSuite; }
	
	// resources\\config.properties
	public ResourceProperties(String fileName) throws InvalidResourcePropertiesException
	{
		Properties prop = new Properties();
		InputStream input = null;
		
		try {
			input = new FileInputStream(fileName);
			prop.load(input);
			
			//this.databaseConnectionString = prop.getProperty("database.connection");
			//this.databaseTimeOut = prop.getProperty("database.timeout");
			//this.databaseUserId = prop.getProperty("database.userid");
			//this.databasePassword = prop.getProperty("database.password");
			//
			this.mqHost = prop.getProperty("mq.host");
			this.mqQueueManager = prop.getProperty("mq.queuemanager");
			this.mqChannel = prop.getProperty("mq.channel");
			this.mqPort = Integer.parseInt(prop.getProperty("mq.port"));
			this.mqUserId = prop.getProperty("mq.userid");
			this.mqPassword = prop.getProperty("mq.password");
			this.mqQueueName = prop.getProperty("mq.queue");
			this.mqUseSSLChannel = prop.getProperty("mq.useSSLChannel");
			
			this.mqCCDTDirectory = prop.getProperty("mq.ccdtDirectory");
			this.mqCCDTFile = prop.getProperty("mq.ccdtFile");
			
			this.keyStoreFilename = prop.getProperty("mq.keystore");
			this.keyStorePass = prop.getProperty("mq.keystorePassword");
			this.trustStoreFilename = prop.getProperty("mq.truststore");
			this.sslCipherSuite = prop.getProperty("mq.sslCipherSuite");
			
		} catch(IOException e) {
			throw new InvalidResourcePropertiesException(3001, e.getMessage());
			
		} finally {
			
	        if (input != null) {
	            try {
	                input.close();
	                
	            } catch (IOException e) {    	
	    			throw new InvalidResourcePropertiesException(3002, e.getMessage());
	            
	            }
	        }
	    }
		
		
	}
}
