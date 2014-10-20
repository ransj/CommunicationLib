/**
 * 
 */
package name.kingbright.group;

import org.alljoyn.bus.annotation.BusInterface;
import org.alljoyn.bus.annotation.BusSignal;

/**
 * @author KingBright
 * 
 */
@BusInterface(name = "name.kingbright.communication.service")
public interface GroupCommunicationInterface {
    @BusSignal
    public void sendMessage(String string) throws Exception;
}
