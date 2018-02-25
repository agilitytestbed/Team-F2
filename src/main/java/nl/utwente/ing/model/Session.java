package nl.utwente.ing.model;

import java.util.Date;

public class Session {

    private String sessionID;
    private Date creationDate;

    public Session(String sessionID, Date creationDate) {
        this.sessionID = sessionID;
        this.creationDate = creationDate;
    }

    public String getSessionID() {
        return sessionID;
    }

    public Date getCreationDate() {
        return creationDate;
    }
}
