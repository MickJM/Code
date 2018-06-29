package maersk.com.mqconnection.v1;

public class MQInvalidMessageException extends Exception {

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
	
	public MQInvalidMessageException()
	{		
	}

	public MQInvalidMessageException(String Message)
	{
		this.Message = Message;
	}

	public MQInvalidMessageException(int ReasonCode, String Message)
	{
		this.ReasonCode = ReasonCode;
		this.Message = Message;
	}

	public MQInvalidMessageException(int CompletionCode, int ReasonCode, String Message)
	{
		this.CompleteionCode = CompletionCode;
		this.ReasonCode = ReasonCode;
		this.Message = Message;
	}

	public MQInvalidMessageException(int CompletionCode, int ReasonCode, Throwable throwable)
	{	
		this.CompleteionCode = CompletionCode;
		this.ReasonCode = ReasonCode;
		
		try {
			this.Message = throwable.getMessage();			
		}
		catch (Exception e) {
			this.Message = "Error returned from MQInvalidMessageException";
		}
	}
	
}
