/**
 * 
 */
package name.kingbright.alljoyn;

import name.kingbright.group.Group;
import name.kingbright.group.GroupCommunicationInterface;
import name.kingbright.group.GroupCreator;
import name.kingbright.group.GroupManager;
import name.kingbright.group.GroupService;

import org.alljoyn.bus.BusAttachment;
import org.alljoyn.bus.BusException;
import org.alljoyn.bus.BusListener;
import org.alljoyn.bus.MessageContext;
import org.alljoyn.bus.Mutable;
import org.alljoyn.bus.SessionListener;
import org.alljoyn.bus.SessionOpts;
import org.alljoyn.bus.SessionPortListener;
import org.alljoyn.bus.SignalEmitter;
import org.alljoyn.bus.Status;
import org.alljoyn.bus.annotation.BusInterface;
import org.alljoyn.bus.annotation.BusSignal;
import org.alljoyn.bus.annotation.BusSignalHandler;

import android.os.Handler;
import android.os.HandlerThread;
import android.os.Looper;
import android.os.Message;
import android.util.Log;

import com.google.gson.Gson;

/**
 * Implementation of {@link GroupService}
 * 
 * @author KingBright
 * 
 */
public class AllJoynGroupService implements GroupService {
    private static final int EXIT = 1;
    private static final int INIT = 2;
    private static final int DISPOSE = 3;
    private static final int START_DISCOVERY = 4;
    private static final int STOP_DISCOVERY = 5;
    private static final int CREATE_GROUP = 6;
    private static final int DISBAND_GROUP = 7;
    private static final int BIND_SESSION = 8;
    private static final int UNBIND_SESSION = 9;
    private static final int ADVERTISE = 10;
    private static final int CANCEL_ADVERTISE = 11;
    private static final int JOIN_GROUP = 12;
    private static final int LEAVE_GROUP = 13;
    private static final int SEND_MESSAGE = 14;

    public static enum BusAttachmentState {
        /** The bus attachment is not connected to the AllJoyn bus */
        DISCONNECTED,
        /** The bus attachment is connected to the AllJoyn bus */
        CONNECTED,
        /** The bus attachment is discovering remote attachments hosting chat channels */
        DISCOVERING
    }

    public static enum HostChannelState {
        /** There is no hosted chat channel */
        IDLE,
        /** The well-known name for the channel has been successfully acquired */
        NAMED,
        /** A session port has been bound for the channel */
        BOUND,
        /** The bus attachment has advertised itself as hosting an chat channel */
        ADVERTISED,
        /** At least one remote device has connected to a session on the channel */
        CONNECTED
    }

    private static final String OBJECT_PATH = "/CommunicationService";

    private static final short CONTACT_PORT = 27;

    private String mApplicationNameSpace = "name.kingbright.communication.service";
    private String mApplicationName = "CommunicationService";
    private BusAttachmentState mBusAttachmentState = BusAttachmentState.DISCONNECTED;
    private GroupDiscoverylistener mGroupChangeListener = new GroupDiscoverylistener();

    /**
     * An attachment on the Bus, needs to be connected to the Bus so that we can do communication with each other.
     */
    private BusAttachment mBusAttachment;

    private BackgroundHandler mBackgroundHandler;

    /**
     * The CommunicationService is the instance of an AllJoyn interface that is exported on the bus and allows us to
     * send signals implementing messages
     */
    private CommunicationService mCommunicationObject = new CommunicationService();

    /**
     * A wrapper of group list and some useful methods for groups.
     */
    private GroupManager mGroupManager = new GroupManager();

    private GroupCreator mGroupCreator;

    public AllJoynGroupService() {
    }

    @Override
    public void setApplicationName(String name) {
        if (name != null && name.trim().length() > 0) {
            mApplicationName = name;
        }
    }

    @Override
    public void setApplicationNameSpace(String namespace) {
        if (namespace != null && namespace.trim().length() > 0) {
            mApplicationNameSpace = namespace;
        }
    }

    @Override
    public String getApplicationName() {
        return mApplicationName;
    }

    @Override
    public String getApplicationNameSpace() {
        return mApplicationNameSpace;
    }

    @Override
    public void setGroupCreator(GroupCreator creator) {
        mGroupCreator = creator;
    }

    @Override
    public void create() {
        HandlerThread busThread = new HandlerThread("BackgroundHandler") {

            @Override
            protected void onLooperPrepared() {
                mBackgroundHandler = new BackgroundHandler(getLooper());
                mBackgroundHandler.init();
            }
        };
        busThread.start();
    }

    @Override
    public void dispose() {
        mBackgroundHandler.dispose();
    }

    @Override
    public void createGroup(String groupName) {
        mBackgroundHandler.createGroup(mApplicationNameSpace + "." + groupName);
    }

    @Override
    public void disbandGroup(String groupName) {
        mBackgroundHandler.disbandGroup();
    }

    @Override
    public void onMemberJoin(int sessionId, String name, short transport) {
        notify("onMemberJoin", name + "(" + sessionId + ") joins. Port : " + transport);
        Group group = mGroupManager.getJoinedGroup(sessionId);
        if (group == null) {
            notify("NoSuchGroup", "no such group (" + name + ")");
            return;
        }
        group.addGroupMember(name, sessionId, transport);
        if (!group.hasCommunicationInterface()) {
            group.createCommunicationInterface(mCommunicationObject);
        }
    }

    @Override
    public void onMemberLeave(int sessionId, String name, short transport) {
        notify("onMemberLeave", name + "(" + sessionId + ") leaves. Port : " + transport);
        Group group = mGroupManager.getJoinedGroup(sessionId);
        if (group == null) {
            notify("NoSuchGroup", "no such group (" + name + ")");
            return;
        }
        group.removeMember(name, sessionId, transport);
    }

    @Override
    public void onMessageReceive(int sessionId, String sender, String message) {
        notify("onMessageReceive", "From " + sender + "(" + sessionId + ") : " + message);
        Group group = mGroupManager.getJoinedGroup(sessionId);
        if (group == null) {
            notify("NoSuchGroup", "no such group");
            return;
        }
        group.dispatchMessage(sessionId, sender, message);
    }

    @Override
    public void sendMessage(String groupName, String message) {
        notify("sendMessage", "Send to group " + groupName + " : " + message);
        mBackgroundHandler.sendMessage(groupName, message);
    }

    @Override
    public void joinGroup(String groupName) {
        notify("joinGroup", "Join group " + groupName);
        mBackgroundHandler.joinGroup(mApplicationNameSpace + "." + groupName);
    }

    @Override
    public void leaveGroup(String groupName) {
        notify("leaveGroup", "Leave group " + groupName);
        int id = mGroupManager.getJoinedGroupId(mApplicationNameSpace + "." + groupName);
        if (id == 0) {
            notify("NoSuchGroup", "no such group");
            return;
        }
        mBackgroundHandler.leaveGroup(id);
    }

    @Override
    public void onRequestGroupInfo(int sessionId, String info) {
        // TODO And here the host will respond this request.
    }

    @Override
    public void requestGroupInfo(String groupName) {
        // TODO When first joined, the member should request for the group
        // info.
    }

    @Override
    public void onGroupInfoGet(String groupName, String info) {
        // TODO When the member got the group info.
    }

    @Override
    public void advertise(String name) {
        notify("advertise", "Advertise group " + name);
        mBackgroundHandler.advertise(mApplicationNameSpace + "." + name);
    }

    @Override
    public void cancelAdvertise(String name) {
        notify("cancelAdvertise", "Cancel advertising group " + name);
        mBackgroundHandler.cancelAdvertise(mApplicationNameSpace + "." + name);
    }

    @Override
    public void startDiscoverGroup() {
        notify("startDiscoverGroup", "Start discovering group");
        mBackgroundHandler.startDiscovery();
    }

    @Override
    public void stopDiscoverGroup() {
        notify("stopDiscoverGroup", "Stop discovering group");
        mBackgroundHandler.stopDiscovery();
    }

    @Override
    public void onGroupFind(String groupName, short transport, String namespace) {
        notify("foundAdvertisedName", "found group : " + groupName + ", transport :" + transport + ", namespace : "
                + namespace);
        // Add to pending group if not exists, or update the current pending group.
        if (mGroupCreator == null) {
            this.notify("onGroupFind", "Set a group creator to create groups.");
            return;
        }
        Group group = mGroupCreator.create(groupName, transport, namespace);
        String name = group.getWellknowName();
        if (!mGroupManager.hasPendingGroup(name)) {
            mGroupManager.addPendingGroup(name, group);
        } else {
            mGroupManager.getPendingGroup(name).update(group);
        }
    }

    @Override
    public void onGroupDisband(String groupName, short transport, String namespace) {
        notify("lostAdvertisedName", "lost group : " + groupName + ", transport :" + transport + ", namespace : "
                + namespace);
        if (mGroupCreator == null) {
            this.notify("onGroupFind", "Set a group creator to create groups.");
            return;
        }
        Group group = mGroupCreator.create(groupName, transport, namespace);
        String name = group.getWellknowName();
        if (mGroupManager.hasPendingGroup(name)) {
            mGroupManager.getPendingGroup(name).dispose();
        }
    }

    /**
     * 
     */
    public void doInit() {
        assert (mBusAttachmentState == BusAttachmentState.DISCONNECTED);
        mBusAttachment = new BusAttachment(mApplicationName, BusAttachment.RemoteMessage.Receive);
        mBusAttachment.useOSLogging(true);
        mBusAttachment.setDebugLevel("ALLJOYN_JAVA", 7);
        mBusAttachment.registerBusListener(mGroupChangeListener);

        Status status = mBusAttachment.registerBusObject(mCommunicationObject, OBJECT_PATH);
        if (status != Status.OK) {
            notify("registerBusObject", status);
            return;
        }
        status = mBusAttachment.connect();
        if (status != Status.OK) {
            notify("connect", status);
            return;
        }
        status = mBusAttachment.registerSignalHandlers(new AllJoynSignalHandler());
        if (status != Status.OK) {
            notify("registerSignalHandlers", status);
            return;
        }

        mBusAttachmentState = BusAttachmentState.CONNECTED;
    }

    public void doDispose() {
        mBackgroundHandler.stopDiscovery();
        mBackgroundHandler.dispose();
        mBackgroundHandler.dispose();
    }

    private void doStartDiscovery() {
        assert (mBusAttachmentState == BusAttachmentState.CONNECTED);
        Status status = mBusAttachment.findAdvertisedName(mApplicationNameSpace);
        if (status == Status.OK) {
            mBusAttachmentState = BusAttachmentState.DISCOVERING;
            notify("doStartDiscovery", "discovering");
            return;
        } else {
            notify("doStartDiscovery", status);
            return;
        }
    }

    private void doStopDiscovery() {
        assert (mBusAttachmentState == BusAttachmentState.CONNECTED);
        Status status = mBusAttachment.cancelFindAdvertisedName(mApplicationNameSpace);
        mBusAttachmentState = BusAttachmentState.CONNECTED;
        if (status == Status.OK) {
            notify("doStartDiscovery", "stop discovering");
            return;
        } else {
            notify("doStopDiscovery", status);
            return;
        }
    }

    private boolean doBindSession() {
        Mutable.ShortValue contactPort = new Mutable.ShortValue(CONTACT_PORT);
        SessionOpts sessionOpts =
                new SessionOpts(SessionOpts.TRAFFIC_MESSAGES, true, SessionOpts.PROXIMITY_ANY,
                        SessionOpts.TRANSPORT_ANY);

        Status status = mBusAttachment.bindSessionPort(contactPort, sessionOpts, new SessionPortListener() {
            public boolean acceptSessionJoiner(short sessionPort, String joiner, SessionOpts sessionOpts) {
                // TODO If need to change the accept rule.
                if (sessionPort == CONTACT_PORT) {
                    AllJoynGroupService.this.notify("acceptSessionJoiner", "accept");
                    return true;
                }
                AllJoynGroupService.this.notify("acceptSessionJoiner", "refused");
                return false;
            }

            public void sessionJoined(short transport, int sessionId, String joiner) {
                onMemberJoin(sessionId, joiner, transport);
            }
        });

        if (status == Status.OK) {
            notify("doBindSession", "OK");
            return true;
        } else {
            notify("doBindSession", status);
            return false;
        }
    }

    private boolean doUnbindSession() {
        Status status = mBusAttachment.unbindSessionPort(CONTACT_PORT);
        if (status == Status.OK) {
            notify("doUnbindSession", "OK");
            // TODO disband the group.
            return true;
        } else {
            notify("doUnbindSession", status);
            return false;
        }
    }

    /**
     * 
     * @param name The well-know name, which can be got from {@link Group#getWellknowName()}
     */
    private void doCreateGroup(String name) {
        if (doBindSession()) {
            Status status = mBusAttachment.requestName(name, BusAttachment.ALLJOYN_REQUESTNAME_FLAG_DO_NOT_QUEUE);
            if (status == Status.OK) {
                notify("doCreateGroup", "OK");
            } else {
                notify("doCreateGroup", status);
            }
        }
    }

    /**
     * 
     * @param name The well-know name, which can be got from {@link Group#getWellknowName()}
     */
    private void doDisbandGroup(String name) {
        if (doUnbindSession()) {
            int stateRelation = mBusAttachmentState.compareTo(BusAttachmentState.DISCONNECTED);
            assert (stateRelation >= 0);
            assert (mBusAttachmentState == BusAttachmentState.CONNECTED || mBusAttachmentState == BusAttachmentState.DISCOVERING);

            mBusAttachment.releaseName(name);
        }
    }

    /**
     * TODO To let others can discover you.
     * 
     * @param name The well-know name, which can be got from {@link Group#getWellknowName()}
     */
    private void doAdvertise(String name) {
        Status status = mBusAttachment.advertiseName(name, SessionOpts.TRANSPORT_ANY);

        if (status == Status.OK) {
            notify("doAdvertise", "OK");
        } else {
            notify("doAdvertise", status);
            return;
        }
    }

    /**
     * @param name The well-know name, which can be got from {@link Group#getWellknowName()}
     */
    private void doCancelAdvertise(String name) {
        Status status = mBusAttachment.cancelAdvertiseName(name, SessionOpts.TRANSPORT_ANY);

        if (status == Status.OK) {
            notify("doCancelAdvertise", "OK");
        } else {
            notify("doCancelAdvertise", status);
            return;
        }
    }

    /**
     * 
     * @param name The well-know name, which can be got from {@link Group#getWellknowName()}
     */
    private void doJoinGroup(String name) {
        short contactPort = CONTACT_PORT;
        SessionOpts sessionOpts =
                new SessionOpts(SessionOpts.TRAFFIC_MESSAGES, true, SessionOpts.PROXIMITY_ANY,
                        SessionOpts.TRANSPORT_ANY);
        Mutable.IntegerValue sessionId = new Mutable.IntegerValue();

        Status status = mBusAttachment.joinSession(name, contactPort, sessionId, sessionOpts, new SessionListener() {
            public void sessionLost(int sessionId, int reason) {
                // TODO Remove the group.
            }
        });

        int useSessionId = 0;
        if (status == Status.OK) {
            useSessionId = sessionId.value;
            notify("doJoinSession", "OK");
        } else {
            // Maybe session host is lost or refused by the host.
            notify("doJoinSession", status);
            return;
        }

        // TODO
        SignalEmitter emitter =
                new SignalEmitter(mCommunicationObject, useSessionId, SignalEmitter.GlobalBroadcast.Off);
        AllJoynCommunicationInterface mCommunicationInterface =
                emitter.getInterface(AllJoynCommunicationInterface.class);
        notify("doJoinSession", "Created the communication interface");
    }

    /**
     * 
     * 
     * @param sessionId The group session id.
     */
    private void doLeaveGroup(int sessionId) {
        Status status = mBusAttachment.leaveSession(sessionId);
        if (status == Status.OK) {
            notify("doCancelAdvertise", "OK");
        } else {
            notify("doCancelAdvertise", status);
        }
    }

    private void doSendMessages(String groupName, String message) {
        try {
            Group group = mGroupManager.getJoinedGroup(groupName);
            if (group == null) {
                notify("NoSuchGroup", "no such group (" + groupName + ")");
                return;
            }
            GroupCommunicationInterface communicationInterface = group.getCommunicationInterface();
            if (communicationInterface != null) {
                communicationInterface.sendMessage(message);
            }
        } catch (Exception e) {
            notify("doSendMessages", e.getMessage());
            e.printStackTrace();
        }
    }

    private void notify(String methodName, Status status) {
        notify(methodName, status.name() + ":" + status.getErrorCode());
    }

    private void notify(String methodName, String msg) {
        // TODO
        Log.i(methodName, msg);
    }

    private final class BackgroundHandler extends Handler {
        public BackgroundHandler(Looper looper) {
            super(looper);
        }

        public void joinGroup(String groupWllknowName) {
            sendMessage(obtainMessage(JOIN_GROUP, groupWllknowName));
        }

        public void leaveGroup(int sessionId) {
            sendMessage(obtainMessage(LEAVE_GROUP, sessionId));
        }

        public void init() {
            sendMessage(obtainMessage(INIT));
        }

        public void dispose() {
            sendMessage(obtainMessage(DISPOSE));
            sendMessage(obtainMessage(EXIT));
        }

        public void createGroup(String groupId) {
            sendMessage(obtainMessage(CREATE_GROUP, groupId));
        }

        public void disbandGroup() {
            sendMessage(obtainMessage(DISBAND_GROUP));
        }

        public void startDiscovery() {
            sendMessage(obtainMessage(START_DISCOVERY));
        }

        public void stopDiscovery() {
            sendMessage(obtainMessage(STOP_DISCOVERY));
        }

        public void sendMessage(String groupId, String message) {
            // TODO send to different groups.
            sendMessage(obtainMessage(SEND_MESSAGE, message));
        }

        public void advertise(String name) {
            sendMessage(obtainMessage(ADVERTISE, name));
        }

        public void cancelAdvertise(String name) {
            sendMessage(obtainMessage(CANCEL_ADVERTISE, name));
        }

        public void handleMessage(Message msg) {
            Log.i("handle message", "msg : " + msg.what);
            switch (msg.what) {
                case INIT:
                    doInit();
                    break;
                case DISPOSE:
                    doDispose();
                    break;
                case START_DISCOVERY:
                    doStartDiscovery();
                    break;
                case STOP_DISCOVERY:
                    doStopDiscovery();
                    break;
                case CREATE_GROUP:
                    doCreateGroup((String) msg.obj);
                    break;
                case DISBAND_GROUP:
                    doDisbandGroup((String) msg.obj);
                    break;
                case BIND_SESSION:
                    doBindSession();
                    break;
                case UNBIND_SESSION:
                    doUnbindSession();
                    break;
                case ADVERTISE:
                    // Advertise the group with its name.
                    doAdvertise((String) msg.obj);
                    break;
                case CANCEL_ADVERTISE:
                    // Cancel advertising the group with its name.
                    doCancelAdvertise((String) msg.obj);
                    break;
                case JOIN_GROUP:
                    // Join a group with its name.
                    doJoinGroup((String) msg.obj);
                    break;
                case LEAVE_GROUP:
                    // leave a group with your session id.
                    doLeaveGroup((Integer) msg.obj);
                    break;
                case SEND_MESSAGE:
                    // send message to target group with its name.
                    doSendMessages("", (String) msg.obj);
                    break;
                case EXIT:
                    getLooper().quit();
                    break;
                default:
                    break;
            }
            Log.i("handle message", "DONE");
        }
    }

    private class AllJoynSignalHandler {
        /**
         * The signal handler for messages received from the AllJoyn bus.
         * 
         * Since the messages sent on a chat channel will be sent using a bus signal, we need to provide a signal
         * handler to receive those signals. This is it. Note that the name of the signal handler has the first letter
         * capitalized to conform with the DBus convention for signal handler names.
         */
        @BusSignalHandler(iface = "name.kingbright.communication.service", signal = "sendMessage")
        public void ReceiveMessage(String message) {
            // TODO dispatch messages.
            MessageContext mc = mBusAttachment.getMessageContext();
            String sender = mc.sender;
            int sessionId = mc.sessionId;
            Gson gson = new Gson();
            gson.toJson(mc);
            onMessageReceive(sessionId, sender, message);
        }
    }

    private class GroupDiscoverylistener extends BusListener {
        public void foundAdvertisedName(String name, short transport, String namespace) {
            onGroupFind(name, transport, namespace);
        }

        public void lostAdvertisedName(String name, short transport, String namespace) {
            onGroupDisband(name, transport, namespace);
        }
    }

    @BusInterface(name = "name.kingbright.communication.service")
    private class CommunicationService implements AllJoynCommunicationInterface {
        @BusSignal
        public void sendMessage(String str) throws BusException {
        }
    }

    static {
        System.loadLibrary("alljoyn_java");
    }

}
