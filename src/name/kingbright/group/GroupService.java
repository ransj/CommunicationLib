/**
 * 
 */
package name.kingbright.group;

/**
 * A service works for group managements. Such as creating a group, joining a group or sending messages to other members
 * in group. It is both host and member.
 * 
 * @author KingBright
 * 
 */
public interface GroupService extends GroupHost, GroupWatcher {
    public void create();

    public void dispose();

    /**
     * @param name
     */
    public void setApplicationName(String name);

    /**
     * @param prefix
     */
    public void setApplicationNameSpace(String namespace);

    public String getApplicationName();

    public String getApplicationNameSpace();

    public void setGroupCreator(GroupCreator creator);

}
