/**
 * 
 */
package name.kingbright.group;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import name.kingbright.base.Observable;
import name.kingbright.base.Observer;

/**
 * @author KingBright
 * 
 */
public class GroupManager implements Observable<GroupManager> {
    /**
     * The joined groups use well-know name as key and only when you successfully joined the target group, you can get
     * the session id.
     */
    private Map<String, Group> mJoinedGroups = new HashMap<String, Group>();

    /**
     * The discovered groups. Use group well-know name as key.
     */
    private Map<String, Group> mPendingGroups = new HashMap<String, Group>();

    private List<Observer<GroupManager>> mObservers = new ArrayList<Observer<GroupManager>>();

    public GroupManager() {

    }

    public boolean hasJoinedGroup(String name) {
        return mJoinedGroups.containsKey(name);
    }

    public void addJoinedGroup(String name, Group group) {
        mJoinedGroups.put(name, group);
        notifyObservers();
    }

    public void removeJoinedGroup(String name) {
        Group group = mJoinedGroups.remove(name);
        group.dispose();
        notifyObservers();
    }

    public boolean hasPendingGroup(String name) {
        return mPendingGroups.containsKey(name);
    }

    public void addPendingGroup(String name, Group group) {
        mPendingGroups.put(name, group);
        notifyObservers();
    }

    public void removePendingGroup(String name) {
        Group group = mPendingGroups.remove(name);
        group.dispose();
        notifyObservers();
    }

    public Collection<Group> getPendingGroupList() {
        return mPendingGroups.values();
    }

    public Collection<Group> getJoinedGroupList() {
        return mJoinedGroups.values();
    }

    /**
     * @param groupName
     */
    public Group getJoinedGroup(Integer sessionId) {
        return mJoinedGroups.get(sessionId);
    }

    public Group getPendingGroup(String name) {
        return null;

    }

    @Override
    public void addObserver(Observer<GroupManager> obs) {
        if (!mObservers.contains(obs)) {
            mObservers.add(obs);
        }
    }

    @Override
    public void removeObserver(Observer<GroupManager> obs) {
        if (mObservers.contains(obs)) {
            mObservers.remove(obs);
        }
    }

    private void notifyObservers() {
        for (Observer<GroupManager> obs : mObservers) {
            obs.update(this);
        }
    }

    /**
     * @param groupName
     */
    public int getJoinedGroupId(String name) {
        if (mJoinedGroups.containsKey(name)) {
            return mJoinedGroups.get(name).getSessionId();
        }
        return 0;
    }
}
