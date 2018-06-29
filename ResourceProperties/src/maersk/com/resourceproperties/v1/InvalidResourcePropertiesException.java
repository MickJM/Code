package maersk.com.resourceproperties.v1;

public class InvalidResourcePropertiesException extends Exception {

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
	
	public InvalidResourcePropertiesException()
	{		
	}
	
	public InvalidResourcePropertiesException(int CompletionCode, int ReasonCode, String Message)
	{
		this.CompleteionCode = CompletionCode;
		this.ReasonCode = ReasonCode;
		this.Message = Message;
	}

	public InvalidResourcePropertiesException(int ReasonCode, String Message)
	{
		this.ReasonCode = ReasonCode;
		this.Message = Message;
	}
	
}
