package deltix.ember.connector.grpc.syneroex.session;

import com.syneroex.*;
import deltix.ember.connector.grpc.syneroex.util.SyneroexMessage;

public interface SyneroexListener {

    void onAppMessage(OrderResponse message);

    void onCreateOrder(CreateOrdersRequest request, CreateOrdersResponse response, SyneroexMessage error);
    void onReplaceOrder(ReplaceOrdersRequest request, ReplaceOrdersResponse response, SyneroexMessage error);
    void onCancelOrder(CancelOrdersRequest request, CancelOrdersResponse response, SyneroexMessage error);
    void onQueryOrder(OrderQueryRequest request, OrderQueryResponse response, SyneroexMessage error);

    void onConnected();
    void onDisconnected();

}
