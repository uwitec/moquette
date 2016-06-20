/*
 * Copyright (c) 2012-2015 The original author or authors
 * ------------------------------------------------------
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * and Apache License v2.0 which accompanies this distribution.
 *
 * The Eclipse Public License is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * The Apache License v2.0 is available at
 * http://www.opensource.org/licenses/apache2.0.php
 *
 * You may elect to redistribute this code under either of these licenses.
 */
package io.moquette.server;

import io.moquette.interception.InterceptHandler;
import io.moquette.interception.messages.InterceptAcknowledgedMessage;
import io.moquette.interception.messages.InterceptConnectMessage;
import io.moquette.interception.messages.InterceptDisconnectMessage;
import io.moquette.interception.messages.InterceptPublishMessage;
import io.moquette.interception.messages.InterceptSubscribeMessage;
import io.moquette.interception.messages.InterceptUnsubscribeMessage;
import io.moquette.parser.proto.messages.AbstractMessage.QOSType;
import io.moquette.parser.proto.messages.PublishMessage;
import io.moquette.server.config.IConfig;
import io.moquette.server.config.MemoryConfig;
import io.moquette.spi.IMessagesStore;
import io.moquette.spi.impl.ProtocolProcessor;

import org.apache.http.NameValuePair;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.impl.client.BasicResponseHandler;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.message.BasicNameValuePair;
import org.fusesource.mqtt.client.*;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import java.io.IOException;
import java.lang.reflect.Field;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.TimeUnit;

import static io.moquette.BrokerConstants.PERSISTENT_STORE_PROPERTY_NAME;
import static io.moquette.BrokerConstants.PORT_PROPERTY_NAME;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

/**
 *
 * @author andrea
 */
public class ServerSubUnsubBugTest {

    Server m_server;
    MQTT m_mqtt;
    BlockingConnection m_subscriber;
    BlockingConnection m_publisher;
    IConfig m_config;
    
    protected void startServer() throws IOException {
        m_server = new Server();
//        final Properties configProps = IntegrationUtils.prepareTestProperties();
        Properties configProps = new Properties();
        configProps.put(PERSISTENT_STORE_PROPERTY_NAME, "");
        configProps.put(PORT_PROPERTY_NAME, "1883");
        m_config = new MemoryConfig(configProps);
        List<InterceptHandler> hdls=new ArrayList<InterceptHandler>();
        hdls.add(new InterceptHandler() {
			
			@Override
			public void onUnsubscribe(InterceptUnsubscribeMessage msg) {
				try {
					Thread.sleep(500);
				} catch (InterruptedException e) {
					e.printStackTrace();
				}
				m_server.cleanSubscription("Subscriber");
			}
			
			@Override
			public void onSubscribe(InterceptSubscribeMessage msg) {
				// TODO Auto-generated method stub
				
			}
			
			@Override
			public void onPublish(InterceptPublishMessage msg) {
				// TODO Auto-generated method stub
				
			}
			
			@Override
			public void onMessageAcknowledged(InterceptAcknowledgedMessage msg) {
				// TODO Auto-generated method stub
				
			}
			
			@Override
			public void onDisconnect(InterceptDisconnectMessage msg) {
				// TODO Auto-generated method stub
				
			}
			
			@Override
			public void onConnect(InterceptConnectMessage msg) {
				// TODO Auto-generated method stub
				
			}
		});
        m_server.startServer(m_config,hdls);
    }

    @Before
    public void setUp() throws Exception {
        startServer();

        m_mqtt = new MQTT();

        m_mqtt.setHost("localhost", 1883);
//        m_mqtt.setUserName("100200886044");
//        m_mqtt.setPassword("6a3279fefae80fffc27eb661b66ffc4e");
        m_mqtt.setCleanSession(false);
    }

    @After
    public void tearDown() throws Exception {
        if (m_subscriber != null) {
            m_subscriber.disconnect();
        }
        
        if (m_publisher != null) {
            m_publisher.disconnect();
        }

        m_server.stopServer();
//        IntegrationUtils.cleanPersistenceFile(m_config);
    }
    
    
    @Test
    public void checkUnsubSubBug() throws Exception {
    
        m_mqtt.setClientId("Subscriber");
        //subscribe to /topic
        m_subscriber = m_mqtt.blockingConnection();
        m_subscriber.connect();
        Topic[] topics = new Topic[]{new Topic("fv/vb/chatroom/2", QoS.EXACTLY_ONCE)};
        m_subscriber.subscribe(topics);
        
        //should be just one registration so a publisher receive one notification
        MQTT mqtt = new MQTT();
        mqtt.setHost("localhost", 1883); 
        mqtt.setClientId("Publisher");
        m_publisher = mqtt.blockingConnection();
        m_publisher.connect();
        PublishMessage  pubMsg= new PublishMessage();
        pubMsg.setPayload(ByteBuffer.wrap("Hello world MQTT!!".getBytes()));
        pubMsg.setQos(QOSType.MOST_ONE);
        pubMsg.setTopicName("fv/vb/chatroom/2");
        m_server.internalPublish(pubMsg);
//        publishToProd1("fv/vb/chatroom/2","Hello world MQTT!!");
//        m_publisher.publish("fv/vb/chatroom/2", "Hello world MQTT!!".getBytes(), QoS.AT_MOST_ONCE, false);
        //read the messages
        
	        Message msg = m_subscriber.receive();
	        msg.ack();
//	        msg = m_subscriber.receive();
//	        msg.ack();
	        assertEquals("Hello world MQTT!!", new String(msg.getPayload()));       
	        pubMsg= new PublishMessage();
	        pubMsg.setPayload(ByteBuffer.wrap("Hello world MQTT!!".getBytes()));
	        pubMsg.setQos(QOSType.MOST_ONE);
	        pubMsg.setTopicName("fv/vb/chatroom/2");
	        m_server.internalPublish(pubMsg);
//	        publishToProd1("fv/vb/chatroom/2","Hello world MQTT!!");
//	        m_publisher.publish("fv/vb/chatroom/2", "Hello world MQTT!!".getBytes(), QoS.AT_MOST_ONCE, false);
	        //read the messages
	        
		         msg = m_subscriber.receive();
		        msg.ack();
		        assertEquals("Hello world MQTT!!", new String(msg.getPayload()));       
        
        m_subscriber.unsubscribe(new String[]{"fv/vb/chatroom/#"});
//        Thread.sleep(1000);
        topics = new Topic[]{new Topic("fv/vb/chatroom/1", QoS.EXACTLY_ONCE)};
        m_subscriber.subscribe(topics);
//        msg = m_subscriber.receive();
//        msg.ack();
//        m_subscriber.disconnect();
        
	        pubMsg= new PublishMessage();
	        pubMsg.setPayload(ByteBuffer.wrap("Hello world MQTT!!".getBytes()));
	        pubMsg.setQos(QOSType.MOST_ONE);
	        pubMsg.setTopicName("fv/vb/chatroom/1");
//	        m_server.internalPublish(pubMsg);
	        publishToProd1("fv/vb/chatroom/1","Hello world MQTT!!");
	//        m_publisher.publish("fv/vb/chatroom/1", "Hello world MQTT!!".getBytes(), QoS.AT_MOST_ONCE, false);
	        //read the messages
	        
		         msg = m_subscriber.receive();
		        msg.ack();
		        assertEquals("Hello world MQTT!!", new String(msg.getPayload()));     
       
	        pubMsg= new PublishMessage();
	        pubMsg.setPayload(ByteBuffer.wrap("Hello world MQTT!!".getBytes()));
	        pubMsg.setQos(QOSType.MOST_ONE);
	        pubMsg.setTopicName("fv/vb/chatroom/2");
//	        m_server.internalPublish(pubMsg);
//	        m_publisher.publish("fv/vb/chatroom/2", "Hello world MQTT!!".getBytes(), QoS.AT_MOST_ONCE, false);
	        publishToProd1("fv/vb/chatroom/1","Hello world MQTT!!");
	        //read the messages
	        
		         msg = m_subscriber.receive();
		        msg.ack();
		        assertEquals("Hello world MQTT!!", new String(msg.getPayload()));  
        
    }
    CloseableHttpClient CLIENT = HttpClientBuilder.create().build();
    private void publishToProd1(String topic,String content){
    	HttpPost post = null;
		CloseableHttpResponse response = null;
		try {
			post = new HttpPost(String.format("http://%s/server/pub", "123.57.140.72:9090"));
			List<NameValuePair> parameters = new ArrayList<NameValuePair>();
			parameters.add(new BasicNameValuePair("topic", topic));
			parameters.add(new BasicNameValuePair("content", content));
			parameters.add(new BasicNameValuePair("qos", "0"));
			parameters.add(new BasicNameValuePair("retain", "false"));

			UrlEncodedFormEntity uefe = new UrlEncodedFormEntity(parameters, "UTF-8");
			post.setEntity(uefe);
			String resp = CLIENT.execute(post,new BasicResponseHandler());			
//			LOGGER.info(String.format("publish success! host = %s, topic = %s, content = %s, qos = %d, retain = %b, result = %s", host, topic, content,
//						qos, retained, resp));
		} catch (ClientProtocolException e) {
			e.printStackTrace();
//			LOGGER.error(String.format("publish error! host = %s, topic = %s, content = %s, qos = %d, retain = %b", host, topic, content, qos, retained), e);
			post.abort();
		} catch (IOException e) {
			e.printStackTrace();
//			LOGGER.error(String.format("publish error! host = %s, topic = %s, content = %s, qos = %d, retain = %b", host, topic, content, qos, retained), e);
			post.abort();
		} finally {
			if (response != null) {
				try {
					response.close();
				} catch (IOException e) {
				}
			}
		}
    }
}