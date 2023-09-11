package deltix.connector.fix;

import deltix.anvil.util.AsciiString;
import deltix.anvil.util.ByteSequence;
import deltix.anvil.util.TypeConstants;
import deltix.anvil.util.annotation.Timestamp;
import deltix.efix.message.builder.MessageBuilder;
import deltix.efix.message.field.OrdStatus;
import deltix.efix.message.field.OrdType;
import deltix.efix.message.field.SecurityType;
import deltix.ember.message.smd.InstrumentType;
import deltix.ember.message.trade.*;

import java.util.concurrent.TimeUnit;


public final class FixUtil {

    private static final int DAY_MS = (int) TimeUnit.DAYS.toMillis(1);
    private static final ByteSequence SECURITY_TYPE_BOND = new AsciiString("BOND");

    public static void addNullableCharSequence(int tag, CharSequence sequence, MessageBuilder builder) {
        if (sequence != null) {
            builder.addString(tag, sequence);
        }
    }

    public static CharSequence getSecurityType(InstrumentType securityType) {
        switch (securityType) {
            case EQUITY:
            case ETF:
            case INDEX:
                return SecurityType.COMMON_STOCK;

            case FX:
                return SecurityType.FOREIGN_EXCHANGE_CONTRACT;

            case FUTURE:
                return SecurityType.FUTURE;

            case OPTION:
                return SecurityType.OPTION;

            case BOND:
                return SECURITY_TYPE_BOND;

            case SYNTHETIC:
                return SecurityType.MULTI_LEG_INSTRUMENT;
        }

        throw new IllegalArgumentException("Unsupported security type " + securityType);
    }

    public static OrderType getOrderType(byte orderType) {
        switch (orderType) {
            case OrdType.MARKET:
                return OrderType.MARKET;

            case OrdType.LIMIT:
                return OrderType.LIMIT;

            case OrdType.STOP:
                return OrderType.STOP;

            case OrdType.STOP_LIMIT:
                return OrderType.STOP_LIMIT;
        }

        if (orderType != TypeConstants.BYTE_NULL) {
            throw new IllegalArgumentException("Unsupported order type: " + orderType);
        }

        return null;
    }

    public static byte getOrderType(OrderType orderType) {
        switch (orderType) {
            case MARKET:
                return OrdType.MARKET;

            case LIMIT:
                return OrdType.LIMIT;

            case STOP:
                return OrdType.STOP;

            case STOP_LIMIT:
                return OrdType.STOP_LIMIT;
        }

        throw new IllegalArgumentException("Unsupported order type: " + orderType);
    }

    public static TimeInForce getTimeInForce(byte timeInForce) {
        switch (timeInForce) {
            case deltix.efix.message.field.TimeInForce.DAY:
                return TimeInForce.DAY;

            case deltix.efix.message.field.TimeInForce.GOOD_TILL_CANCEL:
                return TimeInForce.GOOD_TILL_CANCEL;

            case deltix.efix.message.field.TimeInForce.IMMEDIATE_OR_CANCEL:
                return TimeInForce.IMMEDIATE_OR_CANCEL;

            case deltix.efix.message.field.TimeInForce.FILL_OR_KILL:
                return TimeInForce.FILL_OR_KILL;

            case deltix.efix.message.field.TimeInForce.GOOD_TILL_DATE:
                return TimeInForce.GOOD_TILL_DATE;

            case deltix.efix.message.field.TimeInForce.GOOD_TILL_CROSSING:
                return TimeInForce.GOOD_TILL_CROSSING;

            case deltix.efix.message.field.TimeInForce.AT_THE_OPENING:
                return TimeInForce.AT_THE_OPENING;

            case deltix.efix.message.field.TimeInForce.AT_THE_CLOSE:
                return TimeInForce.AT_THE_CLOSE;
        }

        if (timeInForce != TypeConstants.BYTE_NULL) {
            throw new IllegalArgumentException("Unsupported time in force: " + timeInForce);
        }

        return null;
    }

    public static byte getTimeInForce(TimeInForce timeInForce) {
        switch (timeInForce) {
            case DAY:
                return deltix.efix.message.field.TimeInForce.DAY;

            case GOOD_TILL_CANCEL:
                return deltix.efix.message.field.TimeInForce.GOOD_TILL_CANCEL;

            case IMMEDIATE_OR_CANCEL:
                return deltix.efix.message.field.TimeInForce.IMMEDIATE_OR_CANCEL;

            case FILL_OR_KILL:
                return deltix.efix.message.field.TimeInForce.FILL_OR_KILL;

            case GOOD_TILL_DATE:
                return deltix.efix.message.field.TimeInForce.GOOD_TILL_DATE;

            case GOOD_TILL_CROSSING:
                return deltix.efix.message.field.TimeInForce.GOOD_TILL_CROSSING;

            case AT_THE_OPENING:
                return deltix.efix.message.field.TimeInForce.AT_THE_OPENING;

            case AT_THE_CLOSE:
                return deltix.efix.message.field.TimeInForce.AT_THE_CLOSE;
        }

        throw new IllegalArgumentException("Unsupported time in force: " + timeInForce);
    }

    public static OrderStatus getOrderStatus(byte status) {
        switch (status) {
            case OrdStatus.PENDING_NEW:
                return OrderStatus.PENDING_NEW;

            case OrdStatus.NEW:
                return OrderStatus.NEW;

            case OrdStatus.REJECTED:
                return OrderStatus.REJECTED;

            case OrdStatus.PENDING_CANCEL:
                return OrderStatus.PENDING_CANCEL;

            case OrdStatus.CANCELED:
            case OrdStatus.DONE_FOR_DAY:
                return OrderStatus.CANCELED;

            case OrdStatus.EXPIRED:
                return OrderStatus.EXPIRED;

            case OrdStatus.PENDING_REPLACE:
                return OrderStatus.PENDING_REPLACE;

            case OrdStatus.REPLACED:
                return OrderStatus.REPLACED;

            case OrdStatus.PARTIALLY_FILLED:
                return OrderStatus.PARTIALLY_FILLED;

            case OrdStatus.FILLED:
                return OrderStatus.COMPLETELY_FILLED;

            case OrdStatus.SUSPENDED:
                return OrderStatus.SUSPENDED;
        }

        if (status != TypeConstants.BYTE_NULL) {
            throw new IllegalArgumentException("Unsupported order status: " + status);
        }

        return null;
    }

    public static Side getSide(byte side) {
        switch (side) {
            case deltix.efix.message.field.Side.BUY:
                return Side.BUY;
            case deltix.efix.message.field.Side.SELL:
                return Side.SELL;
            case deltix.efix.message.field.Side.SELL_SHORT:
                return Side.SELL_SHORT;
            case deltix.efix.message.field.Side.SELL_SHORT_EXEMPT:
                return Side.SELL_SHORT_EXEMPT;
        }

        if (side != TypeConstants.BYTE_NULL) {
            throw new IllegalArgumentException("Unsupported side: " + side);
        }

        return null;
    }

    public static byte getSide(Side side) {
        switch (side) {
            case BUY:
                return deltix.efix.message.field.Side.BUY;

            case SELL:
                return deltix.efix.message.field.Side.SELL;

            case SELL_SHORT:
                return deltix.efix.message.field.Side.SELL_SHORT;

            case SELL_SHORT_EXEMPT:
                return deltix.efix.message.field.Side.SELL_SHORT_EXEMPT;
        }

        throw new IllegalArgumentException("Unsupported side: " + side);
    }

    @Timestamp
    public static long getExpireTime(@Timestamp long expireTime, @Timestamp long expireDate) {
        if (expireTime != TypeConstants.TIMESTAMP_NULL) {
            return expireTime;
        }

        if (expireDate != TypeConstants.TIMESTAMP_NULL) {
            return expireDate + (DAY_MS - 1);
        }

        return TypeConstants.TIMESTAMP_NULL;
    }


    public static MultiLegReportingType getMultiLegReportingType(byte multiLegReportingType) {
        switch (multiLegReportingType) {
            case deltix.efix.message.field.MultiLegReportingType.SINGLE_SECURITY:
                return MultiLegReportingType.SINGLE_SECURITY;

            case deltix.efix.message.field.MultiLegReportingType.MULTI_LEG_SECURITY:
                return MultiLegReportingType.MULTI_LEG_SECURITY;

            case deltix.efix.message.field.MultiLegReportingType.INDIVIDUAL_LEG_OF_A_MULTI_LEG_SECURITY:
                return MultiLegReportingType.INDIVIDUAL_LEG_SECURITY;
        }

        if (multiLegReportingType != TypeConstants.BYTE_NULL) {
            throw new IllegalArgumentException("Unsupported MultiLegReportingType: " + multiLegReportingType);
        }

        return null;
    }

}
