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

import io.moquette.server.config.IConfig;
import io.moquette.server.config.MemoryConfig;
import io.moquette.spi.IMessagesStore;
import io.moquette.spi.impl.ProtocolProcessor;

import org.fusesource.mqtt.client.*;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import java.io.IOException;
import java.lang.reflect.Field;
import java.util.Properties;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.TimeUnit;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

/**
 *
 * @author andrea
 */
public class ServerStoreIncreasingBugTest {

    Server m_server;
    MQTT m_mqtt;
    BlockingConnection m_subscriber;
    BlockingConnection m_publisher;
    IConfig m_config;
    
    protected void startServer() throws IOException {
        m_server = new Server();
        final Properties configProps = IntegrationUtils.prepareTestPropeties();
        m_config = new MemoryConfig(configProps);
        m_server.startServer(m_config);
    }

    @Before
    public void setUp() throws Exception {
        startServer();

        m_mqtt = new MQTT();

        m_mqtt.setHost("localhost", 1883);
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
        IntegrationUtils.cleanPersistenceFile(m_config);
    }
    
    
    @Test
    public void checkStoreIncreasing() throws Exception {
        m_mqtt.setClientId("Subscriber");
        //subscribe to /topic
        m_subscriber = m_mqtt.blockingConnection();
        m_subscriber.connect();
        Topic[] topics = new Topic[]{new Topic("/topic", QoS.EXACTLY_ONCE)};
        m_subscriber.subscribe(topics);
//        m_subscriber.disconnect();
        
        //should be just one registration so a publisher receive one notification
        MQTT mqtt = new MQTT();
        mqtt.setHost("localhost", 1883); 
        mqtt.setClientId("Publisher");
        m_publisher = mqtt.blockingConnection();
        m_publisher.connect();
        for(int i=0;i<10;i++){
        	m_publisher.publish("/topic", "Hello world MQTT!!".getBytes(), QoS.EXACTLY_ONCE, false);
        }
        //read the messages
        for(int i=0;i<10;i++){
	        Message msg = m_subscriber.receive();
	        msg.ack();
	        assertEquals("Hello world MQTT!!", new String(msg.getPayload()));
        }
        //no more messages on the same topic will be received
        assertNull(m_subscriber.receive(1, TimeUnit.SECONDS));
        m_subscriber.disconnect();
        m_publisher.disconnect();
        m_server.cleanTopic("/topic");
        // m_processor
        Field f=m_server.getClass().getDeclaredField("m_processor");
        f.setAccessible(true);
        ProtocolProcessor m_processor=(ProtocolProcessor)f.get(m_server);
        f=m_processor.getClass().getDeclaredField("m_messagesStore");
        f.setAccessible(true);
        IMessagesStore m_messagesStore=(IMessagesStore)f.get(m_processor);
        f=m_messagesStore.getClass().getDeclaredField("m_persistentMessageStore");
        f.setAccessible(true);
        ConcurrentMap<String, IMessagesStore.StoredMessage> m_persistentMessageStore=(ConcurrentMap<String, IMessagesStore.StoredMessage>)f.get(m_messagesStore);
        assertEquals(0,m_persistentMessageStore.keySet().size());
        
    }

    /**
     * Connect subscribe to topic and publish on the same topic
     * */
    private BlockingConnection subscribeAndPublish(String topic) throws Exception {
        BlockingConnection conn = m_mqtt.blockingConnection();
        conn.connect();
        Topic[] topics = new Topic[]{new Topic(topic, QoS.AT_MOST_ONCE)};
        conn.subscribe(topics);
        conn.publish(topic, "Hello world MQTT!!".getBytes(), QoS.AT_MOST_ONCE, false);
        //read the message
        Message msg = conn.receive();
        msg.ack();
        assertEquals("Hello world MQTT!!", new String(msg.getPayload()));
        return conn;
    }
}