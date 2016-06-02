package io.moquette.spi;

import java.io.Serializable;
/**
 * This is a DTO used to persist minimal status (clean session and activation status) of
 * a session.
 * */
public class PersistentSession implements Serializable {
    /**
	 * serialVersionUID
	 */
	private static final long serialVersionUID = 1139439933013531934L;
	public final boolean cleanSession;

    public PersistentSession(boolean cleanSession) {
        this.cleanSession = cleanSession;
    }
}
