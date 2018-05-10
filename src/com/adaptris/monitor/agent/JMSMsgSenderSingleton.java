package com.adaptris.monitor.agent;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.time.LocalDateTime;
import java.util.EnumMap;
import java.util.Properties;

import javax.jms.Connection;
import javax.jms.DeliveryMode;
import javax.jms.Destination;
import javax.jms.JMSException;
import javax.jms.MessageProducer;
import javax.jms.Session;
import javax.jms.TextMessage;

import org.apache.activemq.ActiveMQConnectionFactory;

import com.adaptris.monitor.agent.activity.ActivityMap;
import com.adaptris.monitor.agent.activity.ConsumerActivity;
import com.adaptris.monitor.agent.activity.ProducerActivity;
import com.adaptris.monitor.agent.json.ConsumerActivitySerializer;
import com.adaptris.monitor.agent.json.ProducerActivitySerializer;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.annotations.Expose;

/**
 * Singleton class to transmit event data to a JMS queue.
 * @author klairb
 *
 */
public class JMSMsgSenderSingleton implements EventReceiverListener {
	private static final String JMS_PROPERTIES_FILE = "jms.properties";
	
	private enum JMSProperty {
		VENDOR_IMPL("vendorImpl"),
		BROKER_URL("brokerUrls"),
		PRODUCER_QUEUE("producerQueue"),
		USERNAME("username"),
		PASSWORD("password");
		
		private String propertyKey;
		JMSProperty(String propertyKey) {this.propertyKey = propertyKey;}
		public String toString() { return this.propertyKey; }
	}
	
	private static JMSMsgSenderSingleton instance;
	private Connection connection;
	private Session session;
	private MessageProducer producer;
	private Gson gson;
	
	
	private JMSMsgSenderSingleton() throws JMSException {
		EnumMap<JMSProperty, String> jmsConfigMap = readJMSProperties();
		try {
			openJMSConnection(jmsConfigMap);
		} catch (JMSException e) {
			System.err.println("Failed to open JMS connection: " + e.getMessage());
			throw e;
		}
		GsonBuilder gsonBuilder = new GsonBuilder();
		gsonBuilder.registerTypeAdapter(ConsumerActivity.class, new ConsumerActivitySerializer());
		gsonBuilder.registerTypeAdapter(ProducerActivity.class, new ProducerActivitySerializer());
		gsonBuilder.excludeFieldsWithoutExposeAnnotation();
		gsonBuilder.setPrettyPrinting();
		gson = gsonBuilder.create();
	}

	public static synchronized JMSMsgSenderSingleton getInstance() throws JMSException {
		if (instance != null) return instance;
		instance = new JMSMsgSenderSingleton();
		return instance;
	}

	private EnumMap<JMSProperty, String> readJMSProperties() {
		EnumMap<JMSProperty, String> jmsConfigMap = new EnumMap<>(JMSProperty.class);
		Properties prop = new Properties();
		try(InputStream input = new FileInputStream(JMS_PROPERTIES_FILE);) {
			prop.load(input);
			for(JMSProperty jmsProperty : JMSProperty.values()) {
				jmsConfigMap.put(jmsProperty, prop.getProperty(jmsProperty.toString()));
			}
			
		} catch (IOException e) {
			System.err.println("Failed to open jms properties file: " + JMS_PROPERTIES_FILE);
			return null;
		}
		return jmsConfigMap;
	}
	
	private void openJMSConnection(EnumMap<JMSProperty, String> jmsConfigMap) throws JMSException {
		ActiveMQConnectionFactory connectionFactory = new ActiveMQConnectionFactory(jmsConfigMap.get(JMSProperty.BROKER_URL));
		connectionFactory.setUseAsyncSend(true);
		
		// Create a Connection
        connection = connectionFactory.createConnection();
        connection.start();

        // Create a Session
        session = connection.createSession(false, Session.AUTO_ACKNOWLEDGE);

        // Create the destination (Topic or Queue)
        Destination destination = session.createQueue(jmsConfigMap.get(JMSProperty.PRODUCER_QUEUE));

        // Create a MessageProducer from the Session to the Topic or Queue
        producer = session.createProducer(destination);
        producer.setDeliveryMode(DeliveryMode.NON_PERSISTENT);
	}

	// Clean up
	public void close() {
        try {
			session.close();
			connection.close();
		} catch (JMSException e) {
			System.err.println("Failed to close JMS session and/or connection: "+e.getMessage());
		}
	}

	public void sendMessage(String textMsg) throws JMSException {
		// Create a messages
//        String text = "Hello world! From: " + Thread.currentThread().getName() + " : " + this.hashCode();
        TextMessage message = session.createTextMessage(textMsg);

        // Tell the producer to send the message
        System.out.println("Sending message: "+ message.hashCode() + " : " + Thread.currentThread().getName());
        producer.send(message);
	}

	@Override
	public void eventReceived(ActivityMap activityMap) {
		System.out.println("Event received");
		try {
			String json = gson.toJson(new ActivtyWrapper(activityMap));
			sendMessage(json);
		} catch (JMSException e) {
			System.err.println("Failed to send msg to JMS broker");
		}
	}
	
	// Wrapper class to allow us to add a datestamp to the json output
	private class ActivtyWrapper {
		@Expose
		LocalDateTime datetimestamp;
		@Expose
		ActivityMap adapterMetrics;
		
		public ActivtyWrapper(ActivityMap metrics) {
			datetimestamp = LocalDateTime.now();
			adapterMetrics = metrics;
		}
	}
}