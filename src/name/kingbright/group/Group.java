/**
 * 
 */
package name.kingbright.group;

import java.util.ArrayList;
import java.util.List;

import name.kingbright.base.Observable;
import name.kingbright.base.Observer;

/**
 * A class represents a group hosted by {@link GroupHost}
 * 
 * @author KingBright
 * 
 */
public abstract class Group implements Observable<Group> {
    private List<GroupMember> mMembers = new ArrayList<GroupMember>();

    private List<Observer<Group>> mObservers = new ArrayList<Observer<Group>>();

    private String mGroupName = "";
    private short mTransport = 0;
    private String mGroupNameSpace = "";

    private int mSessionId = -1;

    private GroupCommunicationInterface mInterface = null;

    public Group(String name, short transport, String namespace) {
        this.mGroupName = name;
        this.mTransport = transport;
        this.mGroupNameSpace = namespace;
    }

    public Group(String name) {
        mGroupName = name;
    }

    public Group() {
    }

    public String getGroupName() {
        return mGroupName;
    }

    /**
     * We'd better use a human readable name.
     * 
     * @param name
     */
    public void setGroupName(String name) {
        mGroupName = name;
    }

    public String getGroupNamespace() {
        return mGroupNameSpace;
    }

    public void setGroupNamespace(String namespace) {
        mGroupNameSpace = namespace;
    }

    public void setTransport(short transport) {
        this.mTransport = transport;
    }

    public short getTransport() {
        return mTransport;
    }

    /**
     * Return the combination of the name space and the name, connected by a "."
     * 
     * @return
     */
    public String getWellknowName() {
        return mGroupNameSpace + "." + mGroupName;
    }

    public GroupCommunicationInterface getCommunicationInterface() {
        return mInterface;
    }

    public boolean hasCommunicationInterface() {
        return mInterface != null;
    }

    public void setSessionId(int sessionId) {
        this.mSessionId = sessionId;
    }

    public int getSessionId() {
        return mSessionId;
    }

    /**
     * Add a member hasn't been added before.
     * 
     * @param member
     */
    public void addMember(GroupMember member) {
        if (!mMembers.contains(member)) {
            mMembers.add(member);
            notifyObservers();
        }
    }

    public void addGroupMember(String name, int sessionId, short transport) {
        GroupMember member = onCreateGroupMember(name, sessionId, transport);
        addMember(member);
    }

    public void removeMember(String name, int sessionId, short transport) {
        GroupMember member = onCreateGroupMember(name, sessionId, transport);
        removeMember(member);
    }

    /**
     * Remove a member has been added before.
     * 
     * @param member
     */
    public void removeMember(GroupMember member) {
        if (mMembers.contains(member)) {
            mMembers.remove(member);
            notifyObservers();
        }
    }

    public int getMemberCount() {
        return mMembers.size();
    }

    @Override
    public void addObserver(Observer<Group> obs) {
        if (!mObservers.contains(obs)) {
            mObservers.add(obs);
        }
    }

    @Override
    public void removeObserver(Observer<Group> obs) {
        if (mObservers.contains(obs)) {
            mObservers.remove(obs);
        }
    }

    /**
     * Remove all the observer first and then remove all the members.
     */
    public void dispose() {
        mObservers.clear();
        mMembers.clear();
    }

    /**
     * @param group
     */
    public void update(Group group) {
        mGroupName = group.getGroupName();
        mTransport = group.getTransport();
        mGroupNameSpace = group.getGroupNamespace();
        mSessionId = group.getSessionId();
        notifyObservers();
    }

    private void notifyObservers() {
        for (Observer<Group> obs : mObservers) {
            obs.update(this);
        }
    }

    /**
     * Return an interface for the whole group communication.
     * 
     * @param groupId
     */
    public void createCommunicationInterface(Object communicationObject) {
        if (mSessionId <= 0) {
            throw new RuntimeException("Sesssion id is not valid : " + mSessionId);
        }
        if (communicationObject == null) {
            throw new RuntimeException("BusObject is invalid : " + communicationObject);
        }
        mInterface = onCreateCommunicationInterface(communicationObject, mSessionId);
    }

    /**
     * Return an interface for peer to peer communication.
     * 
     * @param communicationObject
     * @param name
     * @return
     */
    public GroupCommunicationInterface getPeerToPeerCommunicationInterface(Object communicationObject, String name) {
        if (mSessionId <= 0) {
            throw new RuntimeException("Sesssion id is not valid : " + mSessionId);
        }
        if (communicationObject == null) {
            throw new RuntimeException("BusObject is invalid : " + communicationObject);
        }
        if (name == null || name.length() == 0) {
            throw new RuntimeException("Target peer name is invalid : " + name);
        }
        GroupCommunicationInterface communicationInterface =
                onCreatePeerToPeerCommunicationInterface(communicationObject, mSessionId, name);
        return communicationInterface;
    }

    protected abstract GroupCommunicationInterface onCreatePeerToPeerCommunicationInterface(Object communicationObejct,
            int sessionId, String name);

    protected abstract GroupCommunicationInterface onCreateCommunicationInterface(Object communicationObject,
            int sessionId);

    protected abstract GroupMember onCreateGroupMember(String name, int sessionId, short transport);

    /**
     * @param sender
     * @param message
     */
    public void dispatchMessage(int sessionId, String sender, String message) {
        for (GroupMember member : mMembers) {
            member.onMessageReceive(sessionId, sender, message);
        }
    }
}
