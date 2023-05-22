package deltix.connector.fix.mapper;

import deltix.anvil.util.AsciiStringBuilder;
import deltix.anvil.util.annotation.Alphanumeric;


public class OrderIdFlyweight {

    protected final AsciiStringBuilder orderId = new AsciiStringBuilder(32);
    protected @Alphanumeric long sourceId;

    public AsciiStringBuilder getOrderId() {
        return orderId;
    }

    public long getSourceId() {
        return sourceId;
    }
}
