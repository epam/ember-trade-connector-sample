package deltix.connector.common.core;

import deltix.anvil.util.annotation.Required;

import java.nio.file.Path;

@SuppressWarnings("unused")
public class BaseTradeConnectorSettings {
    protected @Required String attributeKey;
    protected @Required Path workDir;

    public String getAttributeKey() {
        return attributeKey;
    }

    public void setAttributeKey(String attributeKey) {
        this.attributeKey = attributeKey;
    }

    public Path getWorkDir() {
        return workDir;
    }

    public void setWorkDir(Path workDir) {
        this.workDir = workDir;
    }
}
