package maersk.com.mqconnection.v1;

public class MQConnectionException extends Exception {

	private int CompleteionCode;
	private int ReasonCode;
	private String Message;

	public int GetCompletionCode()
	{
		return this.CompleteionCode;
	}
	public int GetReasonCode()
	{
		return this.ReasonCode;
	}
	public String GetMessage()
	{
		return this.Message;
	}
	
	public MQConnectionException()
	{		
	}

	public MQConnectionException(int CompletionCode, int ReasonCode, String Message)
	{
		this.CompleteionCode = CompletionCode;
		this.ReasonCode = ReasonCode;
		this.Message = Message;
	}

	public MQConnectionException(int CompletionCode, int ReasonCode, Throwable throwable)
	{	
		this.CompleteionCode = CompletionCode;
		this.ReasonCode = ReasonCode;
		
		try {
			this.Message = throwable.getMessage();			
		}
		catch (Exception e) {
			this.Message = "Error returned from MQConnection";
		}
	}
	
}
