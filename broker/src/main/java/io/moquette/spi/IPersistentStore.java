/**
 * 
 */
package io.moquette.spi;

import io.moquette.server.config.IConfig;

/**
 * Persistent Store
 * @author ShimonXin
 *
 */
public interface IPersistentStore {
    
    IMessagesStore messagesStore() ;
    
    ISessionsStore sessionsStore(IMessagesStore msgStore);
    
    void initStore(IConfig props);
    
    void close();
}
