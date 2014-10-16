/**
 * 
 */
package name.kingbright.group;

/**
 * @author KingBright
 * 
 */
public interface GroupCreator {
    public Group create(String groupName, short transport, String namespace);
}
