package name.kingbright.alljoyn;

import name.kingbright.group.GroupCommunicationInterface;

import org.alljoyn.bus.BusException;
import org.alljoyn.bus.annotation.BusInterface;
import org.alljoyn.bus.annotation.BusSignal;

@BusInterface(name = "name.kingbright.communication.service")
public interface AllJoynCommunicationInterface extends GroupCommunicationInterface {
    @BusSignal
    public void sendMessage(String str) throws BusException;
}
