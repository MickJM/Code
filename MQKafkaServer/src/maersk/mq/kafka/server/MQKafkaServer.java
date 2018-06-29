package maersk.mq.kafka.server;

import java.io.IOException;
import java.lang.Thread.UncaughtExceptionHandler;
import java.util.ArrayList;
import java.util.List;

import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import maersk.com.mqconnection.v1.MQConnectionException;
import maersk.mq.kafka.process.MQJMSKafkaProcess;
import maersk.mq.kafka.process.MQKafkaProcess;

/**
 * Main class to process MQ messsages to Kafka 
 *
 */
public class MQKafkaServer {

	private final static Logger logger = Logger.getLogger(MQKafkaServer.class);
	private static MQKafkaProcess mqProcess = null;

	private static MQJMSKafkaProcess mqJMSProcess = null;
	
	protected static boolean bShutdown = false;
	
	/**
	 *
	 * @throws IOException
	 * @throws MQConnectionException 
	 */
	public static void main(String[] args) throws IOException, MQConnectionException {

		/*
		 * Hooks to handle shutdown and unexected faults
		 */
		Runtime.getRuntime().addShutdownHook(new Thread() 
		{

			@Override
			public void run() {
				logger.log(Level.INFO, "Shutdown received in MQKafkaServer");
				System.out.println("Info: Shutdown received in MQKafkaServer");
				
				synchronized (mqProcess) {
					
					bShutdown  = true;
					mqProcess.notify();
					//
					logger.log(Level.INFO, "Info: MQKafkaServer stopping");
					if (mqProcess != null) {
						try {
							mqProcess.StopServer();
						} catch (MQConnectionException e) {
							System.out.println("Error: Error closing MQ connection");
						}
					}
					System.out.println("Info: MQKafkaServer stopped cleanly");
				
				}
			}
		});
	
		Thread.setDefaultUncaughtExceptionHandler(new UncaughtExceptionHandler() {		
			@Override
			public void uncaughtException(Thread t, Throwable e) {
				logger.log(Level.ERROR, "UncaughtException Handler in MQKafkaServer");
				System.out.print("ERROR: UncaughtException Handler in MQKafkaServer\n");
				System.out.print("ERROR: " + e.getMessage());

				synchronized (mqProcess) {
					
					bShutdown  = true;
					mqProcess.notify();
					//
					logger.log(Level.INFO, "Info: MQ Kafka Server stopping");
					if (mqProcess != null) {
						try {
							mqProcess.StopServer();
						} catch (MQConnectionException e1) {
							System.out.println("Error: Error closing MQ connection");
						}
					}
					System.out.println("Info: MQ Kafka Server stopped after Exception");
				
				}

			}
		});

		System.out.println("Info: MQ Kafka Server");
		System.out.println("Info: Running in MQ Mode");

		//logger.log(Level.INFO, "MQ Kafka Server is starting");

			
		try {
			mqProcess = new MQKafkaProcess("MQEvents");
			
		} catch (Exception e) {
			
			System.out.println("Error: error creating mqProcess " + e.getMessage());
		}
		
		if (mqProcess != null) {
			System.out.println("Info: MQ Process created");
		} else {
			System.out.println("Info: MQ Process is null");			
		}
		
	//	mqJMSProcess = new MQJMSKafkaProcess("MQEvents");		

		Thread mqProcessorThread = new Thread(mqProcess);
		mqProcessorThread.start();

		logger.log(Level.INFO, "MQ Kafka Server has started");

		/*
		 * Block the main thread
		 */
		while (!bShutdown) {
			try {
				System.out.println("Info: MQ Kafka Server is processing");

				synchronized (mqProcess) {

					mqProcess.wait();
				}
				
				// Check for 'sync missed signal'
				if (mqProcess != null) {
					if (mqProcess.GetShutDown()) {
						System.out.println("Info: MQ Kafka Server stopping");
						bShutdown = true;
					} else {
						System.out.println("Warning: MQ Kafka Server received a 'sync missed signal' ");
						System.out.println("Warning: Processing is continuing");
					}
				}
			} catch (InterruptedException e) {
				// TODO Auto-generated catch block
				System.out.println("Info: Thread interupted");
				bShutdown = true;
	
			}
		}

		System.out.println("Info: MQ Kafka Server has stopped");
		System.exit(0);
		

	}

}
