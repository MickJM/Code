package maersk.com.mqjms.sendtokafka.v1;

import maersk.ibm.message.hub.v1.MessageHub;

public class SendToKafka extends SendToKafkaBase{

	private MessageHub msgHub = null;

	public void setMsgHub(MessageHub msgHub) {
		this.msgHub = msgHub;
	}

	

}
