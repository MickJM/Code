package maersk.mq.kafka.metrics;

public class MQMetrics {

	private long Processed = 0;
	private long Success = 0;
	private long Failed = 0;
	
	public MQMetrics() {	
	}
	
	public synchronized void AddProcessed() {
		Processed++;
	}

	public synchronized void AddSuccess() {
		Success++;
	}

	public synchronized void AddFailed() {
		Failed++;
	}

	public synchronized long GetProcessed() {
		return this.Processed;
	}
	public synchronized long GetSuccess() {
		return this.Success;
	}
	public synchronized long GetFailed() {
		return this.Failed;
	}

}
