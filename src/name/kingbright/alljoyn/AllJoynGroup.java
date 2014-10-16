/**
 * 
 */
package name.kingbright.alljoyn;

import org.alljoyn.bus.BusObject;
import org.alljoyn.bus.SignalEmitter;

import name.kingbright.group.Group;
import name.kingbright.group.GroupCommunicationInterface;
import name.kingbright.group.GroupMember;

/**
 * @author KingBright
 * 
 */
public class AllJoynGroup extends Group {

    public AllJoynGroup(String name, short transport, String namespace) {
        super(name, transport, namespace);
    }

    @Override
    protected GroupCommunicationInterface onCreateCommunicationInterface(BusObject communicationObject, int sessionId) {
        SignalEmitter emitter = new SignalEmitter(communicationObject, sessionId, SignalEmitter.GlobalBroadcast.Off);
        AllJoynCommunicationInterface communicationInterface =
                emitter.getInterface(AllJoynCommunicationInterface.class);
        return communicationInterface;
    }

    @Override
    protected GroupCommunicationInterface onCreatePeerToPeerCommunicationInterface(BusObject communicationObject,
            int sessionId, String name) {
        SignalEmitter emitter =
                new SignalEmitter(communicationObject, name, sessionId, SignalEmitter.GlobalBroadcast.Off);
        AllJoynCommunicationInterface communicationInterface =
                emitter.getInterface(AllJoynCommunicationInterface.class);
        return communicationInterface;
    }

    @Override
    protected GroupMember onCreateGroupMember(String name, int sessionId, short transport) {
        // TODO
        return null;
    }

}
