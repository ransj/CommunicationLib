/**
 * 
 */
package name.kingbright.group;

/**
 * A member of the group, which will receive all the messages of this group.
 * 
 * @author KingBright
 * 
 */
public abstract class GroupMember implements GroupWatcher {
    private String mName;
    private String mSessionId;
    private short mTransport;

    public GroupMember(String name, String sessionId, short transport) {
        this.mName = name;
        this.mSessionId = sessionId;
        this.mTransport = transport;
    }

    public String getName() {
        return mName;
    }

    public void setName(String name) {
        this.mName = name;
    }

    public String getSessionId() {
        return mSessionId;
    }

    public void setSessionId(String sessionId) {
        this.mSessionId = sessionId;
    }

    public short getTransport() {
        return mTransport;
    }

    public void setTransport(short transport) {
        this.mTransport = transport;
    }

}
