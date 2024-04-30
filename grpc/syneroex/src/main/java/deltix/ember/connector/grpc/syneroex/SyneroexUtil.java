package deltix.ember.connector.grpc.syneroex;

import com.epam.deltix.dfp.Decimal;
import com.epam.deltix.dfp.Decimal64Utils;
import com.google.protobuf.Descriptors;
import com.google.protobuf.Message;
import com.google.protobuf.TextFormat;
import com.google.protobuf.util.JsonFormat;
import com.syneroex.*;
import deltix.ember.message.trade.OrderStatus;
import deltix.ember.message.trade.OrderType;
import deltix.ember.message.trade.Side;
import deltix.ember.message.trade.TimeInForce;
import edu.umd.cs.findbugs.annotations.DefaultAnnotationForParameters;
import edu.umd.cs.findbugs.annotations.NonNull;

import java.util.HashSet;

@DefaultAnnotationForParameters(NonNull.class)
public final class SyneroexUtil {

    public static String messageToString(Message message) {
        return TextFormat.printer().shortDebugString(message);
    }

    public static JsonFormat.Printer createJsonPrinter() {
        final HashSet<Descriptors.FieldDescriptor> fieldsToAlwaysOutput = new HashSet<>();

        fieldsToAlwaysOutput.add(OrderResponse.getDescriptor().findFieldByNumber(OrderResponse.CLIENT_ORDER_ID_FIELD_NUMBER));
        fieldsToAlwaysOutput.add(CreateOrdersResponse.getDescriptor().findFieldByNumber(CreateOrdersResponse.SUCCESS_FIELD_NUMBER));

        return JsonFormat.printer().omittingInsignificantWhitespace().includingDefaultValueFields(fieldsToAlwaysOutput);
    }

    public static DecimalValue toDecimalValue(@Decimal long value) {
        final int scale = Decimal64Utils.getScale(value);
        return DecimalValue.newBuilder().setMantissa(Decimal64Utils.toFixedPoint(value, scale)).setExponent(-scale).build();
    }

    @Decimal
    public static long fromDecimalValue(DecimalValue value) {
        return Decimal64Utils.fromFixedPoint(value.getMantissa(), -value.getExponent());
    }

    public static OrderStatus toOrderStatus(SyneroexOrderState state) {
        switch (state) {
            case ACKNOWLEDGED: return OrderStatus.PENDING_NEW;
            case OPEN: return OrderStatus.NEW;
            case PARTIALLY_FILLED: return OrderStatus.PARTIALLY_FILLED;
            case COMPLETELY_FILLED: return OrderStatus.COMPLETELY_FILLED;
            case REJECTED: return OrderStatus.REJECTED;
            case CANCELLED: return OrderStatus.CANCELED;
            default:
                throw new IllegalArgumentException("Unsupported order state: " + state);
        }
    }


    public static SyneroexSide toSyneroexSide(Side side) {
        switch (side) {
            case BUY: return SyneroexSide.BUY;
            case SELL: return SyneroexSide.SELL;
            default:
                throw new IllegalArgumentException("Unsupported side: " + side);
        }
    }

    public static SyneroexOrderType toSyneroexOrderType(OrderType orderType) {
        switch (orderType) {
            case MARKET: return SyneroexOrderType.MARKET;
            case LIMIT: return SyneroexOrderType.LIMIT;
            default:
                throw new IllegalArgumentException("Unsupported orderType: " + orderType);
        }
    }

    public static SyneroexTimeInForce toSyneroexTIF(TimeInForce tif) {
        switch (tif) {
            case GOOD_TILL_CANCEL:
                return SyneroexTimeInForce.GTC;
            case FILL_OR_KILL:
                return SyneroexTimeInForce.FOK;
            default:
                return SyneroexTimeInForce.UNRECOGNIZED;
        }
    }
}
