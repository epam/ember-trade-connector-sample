package deltix.connector.fix.deltix;

import deltix.anvil.util.annotation.Optional;
import deltix.connector.fix.FixTradeConnectorFactory;
import deltix.efix.FixVersion;
import deltix.efix.endpoint.session.SessionContext;
import deltix.ember.service.connector.TradeConnectorContext;

@SuppressWarnings("unused")
public final class DeltixTradeConnectorFactory extends FixTradeConnectorFactory<DeltixTradeConnector> {

    private @Optional String username;
    private @Optional String password;
    private @Optional String execBrokerId;
    private @Optional boolean cancelOnDisconnect;


    public DeltixTradeConnectorFactory() {
        super(FixVersion.FIX44);
    }

    public void setUsername(final String username) {
        this.username = username;
    }

    public void setPassword(final String password) {
        this.password = password;
    }

    public void setExecBrokerId(String execBrokerId) {
        this.execBrokerId = execBrokerId;
    }

    public void setCancelOnDisconnect(boolean cancelOnDisconnect) {
        this.cancelOnDisconnect = cancelOnDisconnect;
    }

    @Override
    protected DeltixTradeConnector create(final TradeConnectorContext connectorContext,
                                          final SessionContext sessionContext,
                                          final String attributeKey) {
        return new DeltixTradeConnector(connectorContext, sessionContext, attributeKey, username, password, execBrokerId, cancelOnDisconnect);
    }
}
