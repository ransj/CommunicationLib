/**
 * 
 */
package name.kingbright.group;

/**
 * A group host is only used for hosting a group and play a role as watch dog. It will never involve in the
 * communication.
 * 
 * @author KingBright
 * 
 */
public interface GroupHost {
    public void createGroup(String groupId);

    public void disbandGroup(String groupId);

    public void onMemberJoin(int sessionId, String name, short transport);

    public void onMemberLeave(int sessionId, String name, short transport);

    public void onRequestGroupInfo(int sessionId, String name);

    public void advertise(String name);

    public void cancelAdvertise(String name);

}
