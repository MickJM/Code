package maersk.ibm.message.hub.v1;

import java.io.File;
import java.security.InvalidParameterException;
import java.text.DecimalFormat;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.List;

import org.apache.log4j.Logger;

import maersk.eventhub.messaging.base.*;

/**
 * Base class to connect to IBM MessageHub
 * 
 */
public class MessageHubBase extends EventHubMessagingBase {

	private final Logger logger = Logger.getLogger(MessageHubBase.class);
	
	protected List<KafkaServer> kafkaServersList;
	
	protected String Acks;
	protected int Retries;
	protected int BatchSize;
	protected int Linger;
	protected Long BuffMem;
	protected String KeySerializer;
	protected String ValSerializer;
	protected String ClientID;
	protected int MaxBlock;
	protected String SASLServiceName;
	protected String SecurityProtocol;
	protected String SASLMech;
	protected String SSLProtocol;
	protected String SSLEnabledProtocol;
	protected String EndPointAlgor;
	protected String RequestTimeoutMs;
	protected String OffsetCommitTimeoutMs;
	
	protected String jassLocation;
	protected String showEnv;
	protected boolean asyncMode;
	
	public MessageHubBase() throws ParseException,
						InvalidParameterException, 
						NumberFormatException, Exception  {
		
		AssignEnvironmentKafkaServerVariables();
		AssignEnvironmentKSVariables();
		
	}

	public List<KafkaServer> GetServerList() {
		return this.kafkaServersList;
	}

	private void AssignEnvironmentKSVariables() throws ParseException,
				InvalidParameterException, NumberFormatException, Exception {

		showEnv = null;
		try {
			showEnv = System.getenv("EVENTHUB_TRACE");
		}
		catch (Exception e) {
			showEnv = "NONE";
		}
		if (showEnv == null) {
			showEnv = "NONE";
			
		}
		String envField = "";
		// which mode ?
		this.asyncMode = true;
		String mode = null;
		try {
			envField = "EVENTHUB_MH_SYNC_MODE";
			mode = System.getenv(envField);
			if (showEnv.equals("DEBUG")) {
				System.out.printf("DEBUG: %s = %s\n", envField, mode);
			}

			if (mode != null) {
				if (mode.equals("ASYNC")) {
					this.asyncMode = true;
				} else if (mode.equals("SYNC")) {
					this.asyncMode = false;
				}
			} else {
				this.asyncMode = false;
			}
		}
		catch (Exception e) {
			this.asyncMode = false;
		}
		if (showEnv.equals("DEBUG")) {
			if (this.asyncMode) {
				System.out.println("DEBUG: Processing in ASYNC mode");
			} else {
				System.out.println("DEBUG: Processing in SYNC mode");
			}
		}
		
		//
		try {
			envField = "EVENTHUB_MH_KAFKA_ACKS";
			this.Acks = System.getenv(envField);
			if (showEnv.equals("DEBUG")) {
				System.out.printf("DEBUG: %s = %s\n", envField, this.Acks);
			}
			
			envField = "EVENTHUB_MH_KAFKA_RETRIES";
			this.Retries = Integer.parseInt(System.getenv(envField));
			if (showEnv.equals("DEBUG")) {
				System.out.printf("DEBUG: %s = %d\n", envField, this.Retries);
			}

			envField = "EVENTHUB_MH_KAFKA_BATCHSIZE";
			this.BatchSize = Integer.parseInt(System.getenv(envField));
			if (showEnv.equals("DEBUG")) {
				System.out.printf("DEBUG: %s = %d\n", envField, this.BatchSize);
			}
			
			envField = "EVENTHUB_MH_KAFKA_LINGER";
			this.Linger = Integer.parseInt(System.getenv(envField));
			if (showEnv.equals("DEBUG")) {
				System.out.printf("DEBUG: %s = %d\n", envField, this.Linger);			
			}
			
			envField = "EVENTHUB_MH_KAFKA_BUFFMEM";
			this.BuffMem = Long.parseLong(System.getenv(envField));
			if (showEnv.equals("DEBUG")) {
				System.out.printf("DEBUG: %s = %d\n", envField, this.BuffMem);						
			}
			
			envField = "EVENTHUB_MH_KAFKA_KEYSER";
			this.KeySerializer = System.getenv(envField);
			if (showEnv.equals("DEBUG")) {
				System.out.printf("DEBUG: %s = %s\n", envField, this.KeySerializer);						
			}
			
			envField = "EVENTHUB_MH_KAFKA_VALSER";
			this.ValSerializer = System.getenv(envField);
			if (showEnv.equals("DEBUG")) {
				System.out.printf("DEBUG: %s = %s\n", envField, this.ValSerializer);						
			}
			
			envField = "EVENTHUB_MH_KAFKA_CLIENTID";
			this.ClientID = System.getenv(envField);
			if (showEnv.equals("DEBUG")) {
				System.out.printf("DEBUG: %s = %s\n", envField, this.ClientID);									
			}

			envField = "EVENTHUB_MH_KAFKA_MAXBLOCK";
			this.MaxBlock = Integer.parseInt(System.getenv(envField));
			if (showEnv.equals("DEBUG")) {
				System.out.printf("DEBUG: %s = %d\n", envField, this.MaxBlock);												
			}

			envField = "EVENTHUB_MH_KAFKA_SASLSERVICE";
			this.SASLServiceName = System.getenv(envField);
			if (showEnv.equals("DEBUG")) {
				System.out.printf("DEBUG: %s = %s\n", envField, this.SASLServiceName);												
			}
			
			envField = "EVENTHUB_MH_KAFKA_SECPRO";
			this.SecurityProtocol = System.getenv(envField);
			if (showEnv.equals("DEBUG")) {
				System.out.printf("DEBUG: %s = %s\n", envField, this.SecurityProtocol);												
			}
			
			envField = "EVENTHUB_MH_KAFKA_SASLMECH";
			this.SASLMech = System.getenv(envField);
			if (showEnv.equals("DEBUG")) {
				System.out.printf("DEBUG: %s = %s\n", envField, this.SASLMech);												
			}
			
			envField = "EVENTHUB_MH_KAFKA_SSLPRO";
			this.SSLProtocol = System.getenv(envField);
			if (showEnv.equals("DEBUG")) {
				System.out.printf("DEBUG: %s = %s\n", envField, this.SSLProtocol);												
			}
			
			envField = "EVENTHUB_MH_KAFKA_SSLENABLEDPRO";
			this.SSLEnabledProtocol = System.getenv(envField);
			if (showEnv.equals("DEBUG")) {
				System.out.printf("DEBUG: %s = %s\n", envField, this.SSLEnabledProtocol);												
			}
			
			envField = "EVENTHUB_MH_KAFKA_ENDALGOR";
			this.EndPointAlgor = System.getenv(envField);
			if (showEnv.equals("DEBUG")) {
				System.out.printf("DEBUG: %s = %s\n", envField, this.EndPointAlgor);												
			}
			
			envField = "EVENTHUB_MH_KAFKA_REQUEST_TIMEOUT";
			this.RequestTimeoutMs = System.getenv(envField);
			if (showEnv.equals("DEBUG")) {
				System.out.printf("DEBUG: %s = %s\n", envField, this.RequestTimeoutMs);												
			}
	
			envField = "EVENTHUB_MH_KAFKA_OFFSET_COMMIT_TIMEOUT";
			this.OffsetCommitTimeoutMs = System.getenv(envField);
			if (showEnv.equals("DEBUG")) {
				System.out.printf("DEBUG: %s = %s\n", envField, this.OffsetCommitTimeoutMs);												
			}
		}
		catch (NumberFormatException f)
		{
			System.out.printf("Error: NumberFormatException - Invalid field : %s\n", envField);
			throw new NumberFormatException(f.getMessage());
		}

		catch (Exception e)
		{
			System.out.printf("Error: Exception - Invalid field %s\n", envField);
			throw new Exception(e);
		}
		
		Boolean isPublic = true;
		String conn = System.getenv("KAFKA_ENV");
		if (showEnv.equals("DEBUG")) {
			System.out.println("DEBUG: KAFKA_ENV = " + conn);												
		}
		
		if (conn == null) {
			conn = "public";
		}
		if (conn.equals("public")) {
			isPublic = true;
		}
		if (conn.equals("dedicated")) {
			isPublic = false;
		}

		String jLocation = null;
		jLocation = System.getProperty("user.dir");		
		if (isPublic) {
			jLocation = jLocation + File.separator + "sasl" + File.separator + "public-kafka.config";
		} else {
			//jassLocation = "C:\\Users\\mickm\\Documents\\Development\\javabuild\\classFiles\\jaas\\dedicated-kafka.config";
			jLocation = jLocation + File.separator +  "sasl" + File.separator + "dedicated-kafka.config";
		}
		if (showEnv.equals("DEBUG")) {
			System.out.println("DEBUG: java.security.auth.login.config located in : " + jLocation);		
		}
		this.jassLocation = jLocation;
	}

	
	private void AssignEnvironmentKafkaServerVariables() throws InvalidParameterException, Exception {
		
		this.kafkaServersList = new ArrayList<KafkaServer>();

		String server = null;
		String port = null;
		Boolean bServerEnvFound = false;
		Boolean bPortEnvFound = false;
		
		String envFieldServer = null;
		String envFieldPort = null;
		
		for (int i = 1; i< 5; i++) {
			
			StringBuilder sbServer = new StringBuilder();
			StringBuilder sbPort = new StringBuilder();
			
			sbServer.append("EVENTHUB_MH_KAFKA");
			sbPort.append("EVENTHUB_MH_KAFKA");
			
			String num = new DecimalFormat("00").format(i);
			sbServer.append(num);
			sbPort.append(num);
			
			sbServer.append("_SERVER");
			sbPort.append("_PORT");
			
			envFieldServer = sbServer.toString();
			server = System.getenv(envFieldServer);
			
			if (server != null) {
				bServerEnvFound = true;
			} else {
				if (server == null) {
					if (bServerEnvFound) {
						break;
					} else {
						bServerEnvFound = false;			
					}
				}
			}

			envFieldPort = sbPort.toString();
			port = System.getenv(envFieldPort);

			if (port != null) {
				bPortEnvFound = true;
			} else {
				if (port == null) {
					if (bPortEnvFound) {
						break;
					} else {
						bPortEnvFound = false;			
					}
				}
			}

			if ((server != null) && (port != null)) {
				KafkaServer ks = new KafkaServer();
				ks.ServerName = server;
				ks.PortNumber = port;
				kafkaServersList.add(ks);
			}
			
		}
		if (!bServerEnvFound) {
			System.out.printf("Error: Error with environment field %s\n", envFieldServer);
			throw new InvalidParameterException("Error: Kafka Server environment variable is not set : " + envFieldServer ); 
		}
		if (!bPortEnvFound) { 
			System.out.printf("Error: Error with environment field %s\n", envFieldPort);			
			throw new InvalidParameterException("Error: Kafka Port environment variable is not set : " + envFieldServer); 
		}		
		
		
	}
}

