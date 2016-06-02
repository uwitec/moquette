/**
 * 
 */
package io.moquette.spi;

import io.moquette.server.config.IConfig;

import java.io.Serializable;

/**
 * Persistent Store
 * @author ShimonXin
 *
 */
public interface IPersistentStore {
    /**
     * This is a DTO used to persist minimal status (clean session and activation status) of
     * a session.
     * */
    public static class PersistentSession implements Serializable {
        /**
		 * serialVersionUID
		 */
		private static final long serialVersionUID = 1139439933013531934L;
		public final boolean cleanSession;

        public PersistentSession(boolean cleanSession) {
            this.cleanSession = cleanSession;
        }
    }
    
    IMessagesStore messagesStore() ;
    
    ISessionsStore sessionsStore(IMessagesStore msgStore);
    
    void initStore(IConfig props);
    
    void close();
}
