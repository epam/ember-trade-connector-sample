package deltix.ember.connector.grpc.syneroex.config;

import deltix.anvil.util.annotation.Optional;

public class GrpcSettings {
    private @Optional int maxInboundMessageSize;

    public int getMaxInboundMessageSize() {
        return maxInboundMessageSize;
    }

    public void setMaxInboundMessageSize(int maxInboundMessageSize) {
        this.maxInboundMessageSize = maxInboundMessageSize;
    }
}
