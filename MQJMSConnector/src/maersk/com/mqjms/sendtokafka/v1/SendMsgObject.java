package maersk.com.mqjms.sendtokafka.v1;

import maersk.com.mqjms.sendtokafka.v1.SendMessageBase.msgType;

public class SendMsgObject {

	public String msgId = null;
	public String topicName = null;
	public msgType type = msgType.NULL;
	
	//
	public String payLoad = null;
	//
	public byte[] bytePayload = null;

}
