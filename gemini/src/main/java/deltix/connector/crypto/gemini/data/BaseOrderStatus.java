package deltix.connector.crypto.gemini.data;

import com.epam.deltix.dfp.Decimal;

public interface BaseOrderStatus {
    String getOrderId();

    String getEventId();

    String getClientOrderId();

    String getSymbol();

    OrderSide getSide();

    boolean isLive();

    boolean isCancelled();

    long getTimestamp();

    @Decimal long getAvgExecutionPrice();

    @Decimal long getExecutedAmount();

    @Decimal long getRemainingAmount();

    @Decimal long getOriginalAmount();
}
