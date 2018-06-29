package maersk.ibm.message.hub.v1;

import java.security.InvalidParameterException;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;

import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.clients.consumer.OffsetAndMetadata;
import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.Producer;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.clients.producer.RecordMetadata;
import org.apache.kafka.common.PartitionInfo;
import org.apache.kafka.common.TopicPartition;
import org.apache.kafka.common.errors.TimeoutException;
import org.apache.kafka.common.utils.Bytes;

//import maersk.eventhub.mq.connection.v1.JMSMQConnection;

import org.apache.log4j.Level;
import org.apache.log4j.Logger;

public class MessageHub extends MessageHubBase {

	private final Logger logger = Logger.getLogger(MessageHub.class);
	private String conn = null;
	private Properties kafkaProperties = null;
	private Producer<String, String> producer = null;
	private Producer<String,byte[]> producerByte = null;
	
	// Fields for status
	private Properties kafkaConsumerProperties = null;
	private KafkaConsumer<String, byte[]> consumer = null;
	private Map<String, List<PartitionInfo> > topics = null;
	
	public MessageHub() throws InvalidParameterException, Exception {

		SetKafkaProperties();
		CreateMessageHubProducer();
		CreateMessageHubConsumer();
	}
	

	private void SetKafkaProperties() {
		
		SetKafkaProducerProperties();
		SetKafkaConsumerProperties();
		
	}
	
	private void SetKafkaProducerProperties() {
		logger.log(Level.INFO, "Creating Message Hub Connections");
		System.out.println("Info: Creating Message Hub Connection");
		
		this.kafkaProperties = new Properties();
			StringBuilder sbServerList = new StringBuilder();
			int idx = 0;
			for (KafkaServer ks: this.kafkaServersList) {
				if (idx > 0) {
					sbServerList.append(", ");
				}
				sbServerList.append(ks.ServerName);
				sbServerList.append(":" + ks.PortNumber);
				idx++;
			}
			System.out.println("Info: MessageHub End-point: " + sbServerList.toString());
			this.kafkaProperties.put("bootstrap.servers", sbServerList.toString());
			this.kafkaProperties.put("acks", this.Acks);
			this.kafkaProperties.put("retries", this.Retries);
			this.kafkaProperties.put("batch.size", this.BatchSize);
			this.kafkaProperties.put("linger.ms", this.Linger);
			this.kafkaProperties.put("buffer.memory", this.BuffMem);
			this.kafkaProperties.put("key.serializer", this.KeySerializer);
			this.kafkaProperties.put("value.serializer", this.ValSerializer);
			this.kafkaProperties.put("client.id", this.ClientID);
			this.kafkaProperties.put("max.block.ms", this.MaxBlock);
		
			System.out.println("Info: SASLServiceName: " + this.SASLServiceName);
			this.kafkaProperties.put("sasl.protocol", this.SASLServiceName);
			this.kafkaProperties.put("security.protocol", this.SecurityProtocol);
			this.kafkaProperties.put("sasl.mechanism", this.SASLMech);
			this.kafkaProperties.put("ssl.protocol", this.SSLProtocol);
			this.kafkaProperties.put("ssl.enabled.protocol", this.SSLEnabledProtocol);
			this.kafkaProperties.put("ssl.endpoint.identification.algorithm", this.EndPointAlgor);

			this.kafkaProperties.put("request.timeout.ms", this.RequestTimeoutMs);
			this.kafkaProperties.put("offsets.commit.timeout.ms", this.OffsetCommitTimeoutMs);

		System.out.println("Info: sasl file location : " + this.jassLocation);
		System.setProperty("java.security.auth.login.config", this.jassLocation);
	}
	
	private void SetKafkaConsumerProperties() {
		logger.log(Level.INFO, "Creating Message Hub Connections");
		System.out.println("Info: Creating Message Hub Connection");
		
		this.kafkaConsumerProperties = new Properties();
			StringBuilder sbServerList = new StringBuilder();
			int idx = 0;
			for (KafkaServer ks: this.kafkaServersList) {
				if (idx > 0) {
					sbServerList.append(", ");
				}
				sbServerList.append(ks.ServerName);
				sbServerList.append(":" + ks.PortNumber);
				idx++;
			}
			System.out.println("Info: MessageHub End-point: " + sbServerList.toString());
			this.kafkaConsumerProperties.put("bootstrap.servers", sbServerList.toString());
			this.kafkaConsumerProperties.put("acks", this.Acks);
			this.kafkaConsumerProperties.put("acks", this.Acks);
			this.kafkaConsumerProperties.put("retries", this.Retries);
			this.kafkaConsumerProperties.put("batch.size", this.BatchSize);
			this.kafkaConsumerProperties.put("linger.ms", this.Linger);
			this.kafkaConsumerProperties.put("buffer.memory", this.BuffMem);
			this.kafkaConsumerProperties.put("key.deserializer", "org.apache.kafka.common.serialization.StringDeserializer");
			this.kafkaConsumerProperties.put("value.deserializer", "org.apache.kafka.common.serialization.ByteArrayDeserializer");
			this.kafkaConsumerProperties.put("client.id", this.ClientID);
			this.kafkaConsumerProperties.put("max.block.ms", this.MaxBlock);
		
			System.out.println("Info: SASLServiceName: " + this.SASLServiceName);
			this.kafkaConsumerProperties.put("sasl.protocol", this.SASLServiceName);
			this.kafkaConsumerProperties.put("security.protocol", this.SecurityProtocol);
			this.kafkaConsumerProperties.put("sasl.mechanism", this.SASLMech);
			this.kafkaConsumerProperties.put("ssl.protocol", this.SSLProtocol);
			this.kafkaConsumerProperties.put("ssl.enabled.protocol", this.SSLEnabledProtocol);
			this.kafkaConsumerProperties.put("ssl.endpoint.identification.algorithm", this.EndPointAlgor);

			this.kafkaConsumerProperties.put("request.timeout.ms", this.RequestTimeoutMs);
			this.kafkaConsumerProperties.put("offsets.commit.timeout.ms", this.OffsetCommitTimeoutMs);

		System.out.println("Info: sasl file location : " + this.jassLocation);
		System.setProperty("java.security.auth.login.config", this.jassLocation);
		
		
		
	}
	
	private void CreateMessageHubProducer() {
		
		msgType mt = GetValSerializer();
		
		try {
			if (mt == msgType.STRING) {
				this.producer = new KafkaProducer<String,String>(this.kafkaProperties);
			} else if (mt == msgType.BYTES) {
				this.producerByte = new KafkaProducer<String,byte[]>(this.kafkaProperties);
			}
		}
		catch (TimeoutException to)
		{
			throw new TimeoutException("Error: Kafka timeout creating producer :" + to.getMessage());
		}
		catch (Exception e)
		{
			System.out.println("Error: Error creating message hub producer :" + e.getMessage());
			System.exit(1);
		}
	}

	// Created for the 'status'
	private void CreateMessageHubConsumer() {
		
		try {
			this.consumer = new KafkaConsumer<String,byte[]>(this.kafkaConsumerProperties);
			
		} catch (TimeoutException to) {
			throw new TimeoutException("Error: Kafka timeout creating consumer :" + to.getMessage());			
		
		} catch (Exception e) {
			System.out.println("Error: Error creating message hub consumer :" + e.getMessage());
			System.exit(1);
			
		}
		
	}
	
	// Get a list of topics from MessageHub
	public Map<String, List<PartitionInfo>> GetListOfTopics() {
	
		topics = new HashMap<String, List<PartitionInfo>>();
		topics = consumer.listTopics();
		return topics;
		
	}
	

	
	public void SendMessage(SendMsgObject o) 
			throws TimeoutException, InterruptedException, Exception {

		// Check for topic
		if (showEnv.equals("DEBUG")) {
			System.out.println("DEBUG: Checking for topic :" + o.topicName );			
		}
		CheckTopicExists(o.topicName);
		
		if (showEnv.equals("DEBUG")) {
			System.out.println("DEBUG: Topic exists");		
		}

		if (o.type == msgType.STRING) {
			SendMessageAsString(o);
		}
		if (o.type == msgType.BYTES) {
			SendMessageAsBytes(o);
		}
	}

	private void SendMessageAsBytes(SendMsgObject o) 
			throws TimeoutException, InterruptedException, Exception {
	
		try {
			
			if (asyncMode) {
				ProducerBytesAsync(o);
			} else {
				ProducerBytesSync(o);
			}
			
		} catch (TimeoutException e) {
			System.out.println("Error: Timeout sending request : " + e.getMessage());
			throw new TimeoutException(e.getMessage());
			
		} catch (Exception e) {
			System.out.println("Error: Error sending message : " + e.getMessage());
			throw new Exception(e.getMessage());
		}
 	}
	
	// Async method to send byte message
	private void ProducerBytesAsync(SendMsgObject o) {
		
		if (showEnv.equals("DEBUG")) {
			System.out.println("DEBUG: Prepairing to create ASYNC BYTE record producer for : " + o.topicName);
		}
		
        ProducerRecord<String, byte[]> rec = null;
        if (o.msgId != null) {
        	rec = new ProducerRecord<String, byte[]>(o.topicName, o.msgId, o.bytePayload );
        } else {
        	rec = new ProducerRecord<String, byte[]>(o.topicName, o.bytePayload );
        }
        
		if (showEnv.equals("DEBUG")) {
			System.out.println("DEBUG: ASYNC BYTE Producer record created for : " + o.topicName);
		}
        
		this.producerByte.send(rec, new MessageHubProducerCallBack());		
		this.producerByte.flush();
		
	}

	private void ProducerBytesSync(SendMsgObject o) 
			throws InterruptedException, TimeoutException, ExecutionException {
	
		if (showEnv.equals("DEBUG")) {
			System.out.println("DEBUG: Prepairing to create SYNC BYTE record producer for : " + o.topicName);
		}
		
        ProducerRecord<String, byte[]> rec = null;
        if (o.msgId != null) {
        	rec = new ProducerRecord<String, byte[]>(o.topicName, o.msgId, o.bytePayload );
        } else {
        	rec = new ProducerRecord<String, byte[]>(o.topicName, o.bytePayload );
        }
        
		if (showEnv.equals("DEBUG")) {
			System.out.println("DEBUG: BYTE Producer record created for : " + o.topicName);
		}
        
		RecordMetadata recordMetaData = this.producerByte.send(rec).get();
		if (recordMetaData != null) {
    	} else {
    		System.out.println("ERROR: Error sending message to " 
    					+ recordMetaData.topic());
    		throw new TimeoutException("Error: Potential Kafka TimeOutexception send message : " + recordMetaData.topic());
    	}
				
		this.producerByte.flush();
	
	}

	private void SendMessageAsString(SendMsgObject o) 
			throws TimeoutException, InterruptedException, Exception {
		
		// Depending on the kafka call type, send the message aync or sync
		// ... if aync, any errors are caught in the callback
		try {

			if (asyncMode) {
				ProducerStringAsync(o);
			} else {
				ProducerStringSync(o);
			}

		} catch (TimeoutException e) {
			System.out.println("Error: Timeout sending " + e.getMessage());
			throw new TimeoutException(e.getMessage());
			
		} catch (Exception e) {
			System.out.println("Error: Exception " + e.getMessage());
			throw new Exception(e.getMessage());
		}
		
		
	}
	
	private void ProducerStringAsync(SendMsgObject o) {
		
		if (showEnv.equals("DEBUG")) {
			System.out.println("DEBUG: Prepairing to create ASYNC STRING record producer for : " + o.topicName);
		}

        ProducerRecord<String, String> rec = null;
        if (o.msgId != null) {
        	rec = new ProducerRecord<String, String>(o.topicName, o.msgId, o.payLoad );
        } else {
        	rec = new ProducerRecord<String, String>(o.topicName, o.payLoad );
        }
        
		if (showEnv.equals("DEBUG")) {
			System.out.println("DEBUG: ASYNC STRING Producer record created for : " + o.topicName);
		}

		this.producer.send(rec, new MessageHubProducerCallBack());
		this.producer.flush();
	}

	private void ProducerStringSync(SendMsgObject o) 
			throws InterruptedException, TimeoutException, ExecutionException {
	
		if (showEnv.equals("DEBUG")) {
			System.out.println("DEBUG: Prepairing to SYNC create record producer for : " + o.topicName);
		}
		
        ProducerRecord<String, String> rec = null;
        if (o.msgId != null) {
        	rec = new ProducerRecord<String, String>(o.topicName, o.msgId, o.payLoad );
        } else {
        	rec = new ProducerRecord<String, String>(o.topicName, o.payLoad );
        }
        
		if (showEnv.equals("DEBUG")) {
			System.out.println("DEBUG: SYNC BYTE Producer record created for : " + o.topicName);
		}
        
		RecordMetadata recordMetaData = this.producer.send(rec).get();
		if (recordMetaData != null) {
    	} else {
    		System.out.println("ERROR: Error sending message to " 
    					+ recordMetaData.topic());
    		throw new TimeoutException("Error: Potential Kafka TimeOutexception send message : " + recordMetaData.topic());
    	}
		
		this.producer.flush();
	
	}

	
	
	public void CheckTopicExists(String topicName) {
	
		// Check if the topic exists
		msgType mt = GetValSerializer();

		if (mt == msgType.STRING) {
			try {
				List<PartitionInfo> list = this.producer.partitionsFor(topicName);
				if (showEnv.equals("DEBUG")) {
					System.out.println("DEBUG: (CheckingTopicExists) Topic exists : " + topicName);
				}
			} catch (TimeoutException to) {
				//this.producer.close();
				System.out.println("Info: Topic " + topicName + " does not exist  : " + to.getMessage());
				throw new IllegalStateException("Info: Topic name " + topicName + " does not exist");
			}
		} else if (mt == msgType.BYTES) {
			
			try {
				List<PartitionInfo> list = this.producerByte.partitionsFor(topicName);
				if (showEnv.equals("DEBUG")) {
					System.out.println("DEBUG: (CheckingTopicExists) Topic exists : " + topicName);
				}
			} catch (TimeoutException to) {
				//this.producerByte.close();
				System.out.println("Info: Topic " + topicName + " does not exist  : " + to.getMessage());
				throw new IllegalStateException("Info: Topic " + topicName + " does not exist");
			}
			
		}
		
	}
	
	public void CloseMessageHubProducerWithDelay(int time) {

		msgType mt = GetValSerializer();

		if (mt == msgType.STRING) {
			this.producer.close(time, TimeUnit.SECONDS);
		} else if (mt == msgType.BYTES) {
			this.producerByte.close(time, TimeUnit.SECONDS);
		}
	}

	public void CloseMessageHub() {
		
		msgType mt = GetValSerializer();

		if (mt == msgType.STRING) {
			this.producer.close();
		} else if (mt == msgType.BYTES) {
			this.producerByte.close();
		}

	}


	public msgType GetValSerializer() {
		
		if (this.ValSerializer.contains("StringSerializer")) {					
			//return String.class.getTypeName();
			return msgType.STRING;
		}
		if (this.ValSerializer.contains("ByteArraySerializer")) {
			//return Byte.class.getTypeName(); 
			return msgType.BYTES;
		}
		
		return msgType.NULL;
	}

}
