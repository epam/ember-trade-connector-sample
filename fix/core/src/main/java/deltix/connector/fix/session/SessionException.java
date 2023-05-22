package deltix.connector.fix.session;


public class SessionException extends RuntimeException {

    public static final SessionException SESSION_NOT_CONNECTED = new SessionException("Session is not connected");
    public static final SessionException MESSAGE_NOT_FOUND_IN_STORE = new SessionException("Message is not found in message store");

    public SessionException(String message) {
        super(message);
    }

    @Override
    public Throwable fillInStackTrace() {
        return this;
    }

}
