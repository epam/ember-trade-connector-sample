package deltix.connector.crypto.gemini;

import com.epam.deltix.gflog.api.Log;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectReader;
import deltix.anvil.util.Disposable;
import deltix.connector.crypto.gemini.data.DefaultRequest;
import deltix.connector.crypto.gemini.data.Message;
import deltix.ember.message.trade.SessionStatus;
import org.asynchttpclient.RequestBuilder;
import org.asynchttpclient.util.HttpConstants;
import org.asynchttpclient.ws.WebSocket;
import org.asynchttpclient.ws.WebSocketListener;
import org.asynchttpclient.ws.WebSocketUpgradeHandler;

import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

/**
 * WebSocket client subscribes to order update channel and handles incoming order events.
 */
public class GeminiWsClient implements Disposable {

    private final GeminiContext context;
    private final WebSocketEventListener eventListener;
    private final String subscribeUrl;

    private final WebSocketUpgradeHandler upgradeHandler;
    private final ObjectReader messageReader;
    private final ObjectReader messageListReader;

    private final AtomicReference<WebSocket> webSocketRef = new AtomicReference<>(null);
    private final AtomicReference<SessionStatus> statusRef = new AtomicReference<>(null);

    public GeminiWsClient(GeminiContext context, final String websocketUrl, final WebSocketEventListener eventListener) {
        this.context = context;
        this.eventListener = eventListener;

        final String eventFilter = "eventTypeFilter=accepted&eventTypeFilter=rejected&eventTypeFilter=fill&eventTypeFilter=cancelled&eventTypeFilter=cancel_rejected";
        subscribeUrl = websocketUrl + "v1/order/events?" + eventFilter + "&apiSessionFilter=" + context.getApiKeyContext().getApiKey();

        messageReader = context.getObjectMapper().readerFor(new TypeReference<Message>() {});
        messageListReader = context.getObjectMapper().readerFor(new TypeReference<List<Message>>() {});

        GeminiWsListener listener = new GeminiWsListener();
        this.upgradeHandler = new WebSocketUpgradeHandler.Builder().addWebSocketListener(listener).build();
    }

    private Log getLog() {
        return context.getLog();
    }

    private CompletableFuture<WebSocket> createWebSocket() {
        final RequestBuilder requestBuilder = new RequestBuilder();
        requestBuilder
                .setMethod(HttpConstants.Methods.GET)
                .setUrl(subscribeUrl)
                .setHeader("Content-Type", "text/plain");

        final DefaultRequest subscribeRequest = new DefaultRequest();
        subscribeRequest.setRequest("/v1/order/events");
        subscribeRequest.setNonce(context.getApiKeyContext().nextNonce());

        final String payload = JsonUtil.valueAsString(context.getObjectMapper(), subscribeRequest);
        if (getLog().isTraceEnabled()) {
            getLog().trace("WS Subscribe Request: %s").with(payload);
        }

        GeminiUtil.setRequestPayload(context, requestBuilder, payload);

        return context.getHttpClient()
                .prepareRequest(requestBuilder)
                .execute(upgradeHandler).toCompletableFuture()
                .handle((nettyWebSocket, throwable) -> nettyWebSocket);
    }

    private void connectWebSocket() {
        createWebSocket().handle((nettyWebSocket, throwable) -> {
            if (nettyWebSocket != null) {
                getLog().info("Successfully upgraded to websocket session");
            } else {
                getLog().error("WebSocket upgrade failure: %s").with(throwable);
            }
            return null;
        });
    }

    public WebSocket getWebSocket() {
        return webSocketRef.get();
    }

    private void connect() {
        connectWebSocket();
    }

    public void disconnect(String reason) {
        WebSocket websocket = getWebSocket();
        if (websocket != null) {
            getLog().info("Sent websocket close request. Reason: %s").with(reason);
            websocket.sendCloseFrame();
        }
    }

    @Override
    public void open() {
        connect();
    }

    @Override
    public void close() {
        disconnect("Close");
    }

    public boolean isConnected() {
        return statusRef.get() == SessionStatus.CONNECTED;
    }

    private void updateStatus(SessionStatus newStatus) {
        if (statusRef.getAndSet(newStatus) != newStatus) {
            eventListener.onStatusChange(newStatus);
        }
    }

    private void handleWsMessage(String payload) throws Exception {
        final JsonNode response = context.getObjectMapper().readTree(payload);
        if (response.isArray()) {
            List<Message> events = messageListReader.readValue(response);
            for (int i = 0; i < events.size(); i++) {
                Message message = events.get(i);
                eventListener.onMessage(message);
            }
        } else {
            Message event = messageReader.readValue(response);
            eventListener.onMessage(event);
        }
    }

    private void schedulePing() {
        context.getScheduledExecutor().scheduleAtFixedRate(this::sendPing, 5, 10, TimeUnit.SECONDS);
    }

    private void sendPing() {
        WebSocket webSocket = getWebSocket();
        if (webSocket != null) {
            webSocket.sendPingFrame();
        }
    }

    // region Helper Classes

    public interface WebSocketEventListener {
        void onMessage(Message message);
        void onStatusChange(SessionStatus webSocketStatus);
    }

    private final class GeminiWsListener implements WebSocketListener {
        @Override
        public void onOpen(WebSocket websocket) {
            getLog().info("WebSocket is successfully opened");
            webSocketRef.set(websocket);
            schedulePing();
            updateStatus(SessionStatus.CONNECTED);
        }

        @Override
        public void onClose(WebSocket websocket, int code, String reason) {
            getLog().warn("WebSocket has been closed");
            webSocketRef.set(null);
            updateStatus(SessionStatus.DISCONNECTED);
        }

        @Override
        public void onError(Throwable t) {
            getLog().error("WebSocket error: %s").with(t);
            webSocketRef.set(null);
            updateStatus(SessionStatus.DISCONNECTED);
        }

        @Override
        public void onPingFrame(byte[] payload) {
            WebSocket webSocket = getWebSocket();
            if (webSocket != null) {
                webSocket.sendPongFrame(payload);
            }
        }

        public void onTextFrame(String payload, boolean finalFragment, int rsv) {
            if (getLog().isTraceEnabled()) {
                getLog().trace("WebSocket: %s").with(payload);
            }
            if (payload == null || payload.isEmpty()) {
                return;
            }
            if (!finalFragment) {
                getLog().warn("We got fragmented message but they are not supported");
            }
            try {
                handleWsMessage(payload);
            } catch (Exception e) {
                getLog().error("Payload handling failure: %s %s").with(payload).with(e);
            }
        }
    }

    // endregion
}
