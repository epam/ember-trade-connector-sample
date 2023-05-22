package deltix.connector.common;

public class RejectedRequestException extends RuntimeException {

    private final int rejectCode;
    private final boolean isVendor;

    RejectedRequestException(String cause, int deltixRejectCode) {
        this(cause, deltixRejectCode, false);
    }

    RejectedRequestException(String cause, int rejectCode, boolean isVendor) {
        super(cause);
        this.rejectCode = rejectCode;
        this.isVendor = isVendor;
    }

    public int getRejectCode() {
        return rejectCode;
    }

    public boolean isVendor() {
        return isVendor;
    }

}
