package deltix.ember.connector.grpc.syneroex.session;

public enum SessionStatus {

    DISCONNECTED,


    AUTH_SENT,

    AUTH_ACKNOWLEDGED,

    AUTH_REJECTED,


    SUBSCRIBE_SENT,

    SUBSCRIBE_REJECTED,


    UNSUBSCRIBE_SENT,

    UNSUBSCRIBE_REJECTED,


    APPLICATION_CONNECTED;

    public boolean isConnecting() {
        return this == AUTH_SENT ||
               this == AUTH_ACKNOWLEDGED ||
               this == SUBSCRIBE_SENT;
    }

    public boolean isDisconnecting() {
        return this == UNSUBSCRIBE_SENT;
    }
}
