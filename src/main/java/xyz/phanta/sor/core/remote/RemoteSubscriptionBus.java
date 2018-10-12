package xyz.phanta.sor.core.remote;

abstract class RemoteSubscriptionBus {

    protected final RemoteApiImpl api;
    private boolean listening = false;

    RemoteSubscriptionBus(RemoteApiImpl api) {
        this.api = api;
    }

    void checkListening() {
        if (!listening) {
            listening = true;
            enableListen(api.getConnection());
        }
    }

    abstract void enableListen(CoreConnection connection);

}
