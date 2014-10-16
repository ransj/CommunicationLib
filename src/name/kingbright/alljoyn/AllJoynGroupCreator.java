/**
 * 
 */
package name.kingbright.alljoyn;

import name.kingbright.group.Group;
import name.kingbright.group.GroupCreator;

/**
 * @author KingBright
 * 
 */
public class AllJoynGroupCreator implements GroupCreator {

    @Override
    public Group create(String groupName, short transport, String namespace) {
        AllJoynGroup group = new AllJoynGroup(groupName, transport, namespace);
        return group;
    }

}
