package maersk.ibm.message.hub.v1;

import maersk.eventhub.messaging.base.EventHubMessagingBase.msgType;

@SuppressWarnings("unused")
public class SendMsgObject {

	public String msgId = null;
	public String topicName = null;
	public msgType type = msgType.NULL;
	
	//
	public String payLoad = null;
	//
	public byte[] bytePayload = null;
	
}
