package org.mobicents.servlet.sip.testsuite.callcontroller;

import java.util.Properties;

import javax.sip.message.Response;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.cafesip.sipunit.SipCall;
import org.cafesip.sipunit.SipPhone;
import org.cafesip.sipunit.SipStack;
import org.mobicents.servlet.sip.SipServletTestCase;

public class CallForwardingSipUnitTest extends SipServletTestCase {

	private static Log logger = LogFactory.getLog(CallForwardingSipUnitTest.class);

	private SipStack sipStackSender;
	private SipPhone sipPhoneSender;	
	
	private SipStack sipStackReceiver;
	private SipPhone sipPhoneReceiver;

	private static final int TIMEOUT = 5000;	
//	private static final int TIMEOUT = 1000000;

	public CallForwardingSipUnitTest(String name) {
		super(name);
	}

	@Override
	public void setUp() throws Exception {
		super.setUp();
		SipStack.setTraceEnabled(true);
	}

	@Override
	public void tearDown() throws Exception {		
		sipPhoneSender.dispose();		
		sipStackSender.dispose();		
		sipPhoneReceiver.dispose();		
		sipStackReceiver.dispose();
		super.tearDown();
	}

	@Override
	public void deployApplication() {
		assertTrue(tomcat.deployContext(
				projectHome + "/sip-servlets-test-suite/applications/call-forwarding-b2bua-servlet/src/main/sipapp",
				"sip-test-context", 
				"sip-test"));
	}

	@Override
	protected String getDarConfigurationFile() {
		return "file:///"
				+ projectHome
				+ "/sip-servlets-test-suite/testsuite/src/test/resources/"
				+ "org/mobicents/servlet/sip/testsuite/callcontroller/call-forwarding-b2bua-servlet-dar.properties";
	}

	public SipStack makeStack(String transport, int port) throws Exception {
		Properties properties = new Properties();
		String peerHostPort1 = "127.0.0.1:5070";
		properties.setProperty("javax.sip.OUTBOUND_PROXY", peerHostPort1 + "/"
				+ "udp");
		properties.setProperty("javax.sip.STACK_NAME", "UAC_" + transport + "_"
				+ port);
		properties.setProperty("sipunit.BINDADDR", "127.0.0.1");
		properties.setProperty("gov.nist.javax.sip.DEBUG_LOG",
				"logs/callforwarding_debug_" + port + ".txt");
		properties.setProperty("gov.nist.javax.sip.SERVER_LOG",
				"logs/callforwarding_server_" + port + ".txt");
		
		return new SipStack(transport, port, properties);		
	}

	public void setupPhone() throws Exception {
			sipStackSender = makeStack(SipStack.PROTOCOL_UDP, 5080);					
			sipPhoneSender = sipStackSender.createSipPhone("localhost",
					SipStack.PROTOCOL_UDP, 5070, "sip:forward-sender@sip-servlets.com");		
			sipStackReceiver = makeStack(SipStack.PROTOCOL_UDP, 5090);					
			sipPhoneReceiver = sipStackReceiver.createSipPhone("localhost",
					SipStack.PROTOCOL_UDP, 5070, "sip:forward-receiver@sip-servlets.com");
	}

	public void init() throws Exception {
		setupPhone();		
	}

	// Check if we receive the forwarded call for our invite
	public void testCallForwarding() throws Exception {
		init();
		
		SipCall sender = sipPhoneSender.createSipCall();
		SipCall receiver  = sipPhoneReceiver.createSipCall();		
		
		receiver.listenForIncomingCall();
		Thread.sleep(300);
		sender.initiateOutgoingCall("sip:receiver@sip-servlets.com", null);
				
		assertTrue(receiver.waitForIncomingCall(TIMEOUT));					
		assertTrue(receiver.sendIncomingCallResponse(Response.OK, "OK", 0));
		assertTrue(sender.waitOutgoingCallResponse(TIMEOUT));
		assertTrue(receiver.waitForAck(TIMEOUT));					
		assertTrue(sender.sendInviteOkAck());		
		assertTrue(sender.disconnect());
		assertTrue(receiver.waitForDisconnect(TIMEOUT));
		assertTrue(receiver.respondToDisconnect());
	}
}
