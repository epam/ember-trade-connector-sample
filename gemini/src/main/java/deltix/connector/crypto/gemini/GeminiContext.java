package deltix.connector.crypto.gemini;

import com.epam.deltix.gflog.api.Log;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.asynchttpclient.AsyncHttpClient;
import org.asynchttpclient.DefaultAsyncHttpClientConfig;
import org.asynchttpclient.Dsl;

import java.util.concurrent.ScheduledExecutorService;

public class GeminiContext {
    private final ApiKeyContext apiKeyContext;
    private final ObjectMapper objectMapper;
    private final AsyncHttpClient httpClient;
    private final ScheduledExecutorService scheduledExecutor;
    private final Log log;

    public GeminiContext(int webSocketMaxFrameSize, ScheduledExecutorService scheduledExecutor, ApiKeyContext apiKeyContext, Log log) {
        this.objectMapper = createDefaultObjectMapper();
        this.httpClient = createHttpClient(webSocketMaxFrameSize);
        this.scheduledExecutor = scheduledExecutor;
        this.apiKeyContext = apiKeyContext;
        this.log = log;
    }

    private AsyncHttpClient createHttpClient(int maxFrameSize) {
        DefaultAsyncHttpClientConfig.Builder httpClientConfig = Dsl.config();
        httpClientConfig.setKeepAlive(true);
        httpClientConfig.setTcpNoDelay(true);
        httpClientConfig.setMaxConnections(128);
        httpClientConfig.setIoThreadsCount(2);

        if (maxFrameSize > 0)
            httpClientConfig.setWebSocketMaxFrameSize(maxFrameSize);

        return Dsl.asyncHttpClient(httpClientConfig);
    }

    private ObjectMapper createDefaultObjectMapper() {
        ObjectMapper objectMapper = JsonUtil.createBaseObjectMapper();
        JsonUtil.initObjectMapper(objectMapper);
        return objectMapper;
    }

    public ObjectMapper getObjectMapper() {
        return objectMapper;
    }

    public AsyncHttpClient getHttpClient() {
        return httpClient;
    }

    public ApiKeyContext getApiKeyContext() {
        return apiKeyContext;
    }

    public ScheduledExecutorService getScheduledExecutor() {
        return scheduledExecutor;
    }

    public Log getLog() {
        return log;
    }
}
