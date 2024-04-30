package deltix.ember.connector.grpc.syneroex.config;

import deltix.anvil.util.annotation.Optional;

import java.nio.file.Path;

public class LogSettings {
    private @Optional Path logFile;
    private @Optional int logBufferSize = 8 * 1024 * 1024;
    private @Optional int logMaxFiles = 30;
    private @Optional long logMaxFileSize = 1024 * 1024 * 1024;

    public Path getLogFile() {
        return logFile;
    }

    public void setLogFile(Path logFile) {
        this.logFile = logFile;
    }

    public int getLogBufferSize() {
        return logBufferSize;
    }

    public void setLogBufferSize(int logBufferSize) {
        this.logBufferSize = logBufferSize;
    }

    public int getLogMaxFiles() {
        return logMaxFiles;
    }

    public void setLogMaxFiles(int logMaxFiles) {
        this.logMaxFiles = logMaxFiles;
    }

    public long getLogMaxFileSize() {
        return logMaxFileSize;
    }

    public void setLogMaxFileSize(long logMaxFileSize) {
        this.logMaxFileSize = logMaxFileSize;
    }
}
