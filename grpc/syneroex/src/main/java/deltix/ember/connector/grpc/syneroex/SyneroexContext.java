package deltix.ember.connector.grpc.syneroex;

import deltix.anvil.util.annotation.Alphanumeric;

import java.nio.file.Path;

public class SyneroexContext {
    private final String attributeKey;
    private final Path workDir;
    private final @Alphanumeric long eventExchange;

    public SyneroexContext(String attributeKey, Path workDir, long eventExchange) {
        this.attributeKey = attributeKey;
        this.workDir = workDir;
        this.eventExchange = eventExchange;
    }

    public String getAttributeKey() {
        return attributeKey;
    }

    public Path getWorkDir() {
        return workDir;
    }

    public long getEventExchange() {
        return eventExchange;
    }
}
