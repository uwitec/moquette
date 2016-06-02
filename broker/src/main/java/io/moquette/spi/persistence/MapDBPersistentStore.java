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

package io.moquette.spi.persistence;

import io.moquette.server.config.IConfig;
import io.moquette.spi.IMessagesStore;
import io.moquette.spi.IPersistentStore;
import io.moquette.spi.ISessionsStore;
import io.moquette.parser.proto.MQTTException;
import org.mapdb.DB;
import org.mapdb.DBMaker;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import static io.moquette.BrokerConstants.PERSISTENT_STORE_PROPERTY_NAME;
import static io.moquette.BrokerConstants.AUTOSAVE_INTERVAL_PROPERTY_NAME;

/**
 * MapDB main persistence implementation
 */
public class MapDBPersistentStore implements IPersistentStore{

    private static final Logger LOG = LoggerFactory.getLogger(MapDBPersistentStore.class);

    private DB m_db;
    private String m_storePath;
    private int m_autosaveInterval; // in seconds

    protected final ScheduledExecutorService m_scheduler = Executors.newScheduledThreadPool(1);

    public MapDBPersistentStore() {        
    }

    /**
     * Factory method to create message store backed by MapDB
     * */
    public IMessagesStore messagesStore() {
        //TODO check m_db is valid and
        IMessagesStore msgStore = new MapDBMessagesStore(m_db);
        msgStore.initStore();
        return msgStore;
    }

    public ISessionsStore sessionsStore(IMessagesStore msgStore) {
        ISessionsStore sessionsStore = new MapDBSessionsStore(m_db, msgStore);
        sessionsStore.initStore();
        return sessionsStore;
    }
    
    public void initStore(IConfig props) {
    	this.m_storePath = props.getProperty(PERSISTENT_STORE_PROPERTY_NAME, "");
        this.m_autosaveInterval = Integer.parseInt(props.getProperty(AUTOSAVE_INTERVAL_PROPERTY_NAME, "30"));
        if (m_storePath == null || m_storePath.isEmpty()) {
            m_db = DBMaker.newMemoryDB().make();
        } else {
            File tmpFile;
            try {
                tmpFile = new File(m_storePath);
                boolean fileNewlyCreated = tmpFile.createNewFile();
                LOG.info("Starting with {} [{}] db file", fileNewlyCreated ? "fresh" : "existing", m_storePath);
            } catch (IOException ex) {
                LOG.error(null, ex);
                throw new MQTTException("Can't create temp file for subscriptions storage [" + m_storePath + "]", ex);
            }
            m_db = DBMaker.newFileDB(tmpFile).make();
        }
        m_scheduler.scheduleWithFixedDelay(new Runnable() {
            @Override
            public void run() {
                m_db.commit();
            }
        }, this.m_autosaveInterval, this.m_autosaveInterval, TimeUnit.SECONDS);
    }

    public void close() {
        if (this.m_db.isClosed()) {
            LOG.debug("already closed");
            return;
        }
        this.m_db.commit();
        //LOG.debug("persisted subscriptions {}", m_persistentSubscriptions);
        this.m_db.close();
        LOG.debug("closed disk storage");
        this.m_scheduler.shutdown();
        LOG.debug("Persistence commit scheduler is shutdown");
    }
}
