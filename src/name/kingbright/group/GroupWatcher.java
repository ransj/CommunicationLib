/**
 * 
 */
package name.kingbright.group;

/**
 * @author KingBright
 * 
 */
public interface GroupWatcher {
    public void onMessageReceive(int sessionId, String sender, String message);

    public void sendMessage(String groupName, String message);

    public void joinGroup(String groupName);

    public void leaveGroup(String groupName);

    public void requestGroupInfo(String groupName);

    public void onGroupInfoGet(String groupName, String info);

    public void startDiscoverGroup();

    public void stopDiscoverGroup();

    public void onGroupFind(String groupName, short transport, String namespace);

    public void onGroupDisband(String groupName, short transport, String namespace);
}
