package maersk.com.mqparameters.v1;

import java.net.URL;

public class MQParameters {

	private String sHostName;
	private String sQueueManager;
	private String sChannelName;
	private int iPort;
	private String sUserId;
	private String sPassword;
	
	private String ccdtDirectory;
	private String ccdtFile;
	private URL ccdtFileName;
	private boolean UsingCCDT;
	private boolean useSSLChannel;
	
	private String trustStore;
	private String keyStore;
	private String keyStorePass;
	private String sslCipherSuite;
	
	public MQParameters(String Host, String QM, String Channel, int Port,
			String Userid, String Password)
	{
		this.sHostName = Host;
		this.sQueueManager = QM;
		this.sChannelName = Channel;
		this.iPort = Port;
		
		this.sUserId = Userid;
		this.sPassword = Password;
		this.UsingCCDT = false;
		this.useSSLChannel = false;
		
	}
	
	public MQParameters() {
	}
	
	public void SetHost(String value) {
		this.sHostName = value;
	}
	public String GetHost()
	{
		return this.sHostName;
	}
	
	public void SetQM(String value) {
		this.sQueueManager = value;
	}
	public String GetQM()
	{
		return this.sQueueManager;
	}

	public void SetChannel(String value) {
		this.sChannelName = value;
	}
	public String GetChannel()
	{
		return this.sChannelName;
	}

	public void SetPort(int value) {
		this.iPort = value;
	}
	public int GetPort()
	{
		return this.iPort;
	}

	public void SetUserId(String value) {
		this.sUserId = value;
	}
	public String GetUserId()
	{
		return this.sUserId;
	}

	public void SetPassword(String value) {
		this.sPassword = value;
	}
	public String GetPassword()
	{
		return this.sPassword;
	}
	
	public String GetCCDTDirectory() {
		return ccdtDirectory;
	}

	public void SetCCDTDirectory(String ccdtDirectory) {
		this.ccdtDirectory = ccdtDirectory;
	}

	public String GetCCDTFile() {
		return ccdtFile;
	}

	public void SetCCDTFile(String ccdtFile) {
		this.ccdtFile = ccdtFile;
	}

	public URL GetCCDTFileName() {
		return ccdtFileName;
	}

	private void SetCCDTFileName(URL ccdtFileName) {
		this.ccdtFileName = ccdtFileName;
	}

	public boolean GetUsingCCDT() {
		return UsingCCDT;
	}

	public void SetUsingCCDT(boolean usingCCDT) {
		this.UsingCCDT = usingCCDT;
	}


	public boolean GetUseSSLChannel() {
		return useSSLChannel;
	}

	public void SetUseSSLChannel(boolean sslChannel) {
		this.useSSLChannel = sslChannel;
	}
	
	
	
	public String GetTrustStore() {
		return this.trustStore;
	}
	public void SetTrustStore(String params) {
		this.trustStore = params;
	}

	public String GetKeyStore() {
		return this.keyStore;
	}
	public void SetKeyStore(String params) {
		this.keyStore = params;
	}
	
	public String GetKeyPass() {
		return this.keyStorePass;
	}
	public void SetKeyPass(String params) {
		this.keyStorePass = params;
	}

	public String GetCipherSuite() {
		return this.sslCipherSuite;
	}
	public void SetCipherSuite(String params) {
		this.sslCipherSuite = params;
	}
	
	
}
