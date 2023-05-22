package deltix.connector.crypto.gemini;

import com.epam.deltix.gflog.api.Log;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.type.TypeFactory;
import deltix.connector.crypto.gemini.data.*;
import org.asynchttpclient.RequestBuilder;
import org.asynchttpclient.Response;
import org.asynchttpclient.util.HttpConstants;

import java.util.function.BiConsumer;
import java.util.function.BiFunction;

/**
 * HTTP client execute various REST API requests.
 * <a href="https://docs.gemini.com/rest-api/">Gemini REST API</a>
 * <p>
 * Important Notes:
 * Authenticated APIs do not submit their payload as POSTed data, but instead put it in the X-GEMINI-PAYLOAD header
 */
public class GeminiHttpClient {
    private final static JavaType ORDER_STATUS_TYPE = TypeFactory.defaultInstance().constructType(new TypeReference<OrderStatus>(){});

    private final GeminiContext context;

    private final String newOrderUrl;
    private final String cancelOrderUrl;
    private final String orderStatusUrl;
    private final String heartbeatUrl;

    public GeminiHttpClient(GeminiContext context, String restUrl) {
        this.context = context;

        newOrderUrl    = restUrl + "v1/order/new";
        cancelOrderUrl = restUrl + "v1/order/cancel";
        orderStatusUrl = restUrl + "v1/order/status";
        heartbeatUrl   = restUrl + "v1/heartbeat";
    }

    private Log getLog() {
        return context.getLog();
    }

    public void newOrder(NewOrderRequest request, BiConsumer<OrderStatus, Throwable> callback) {
        request.setRequest("/v1/order/new");
        request.setNonce(context.getApiKeyContext().nextNonce());

        final RequestBuilder requestBuilder = createRequestBuilder(newOrderUrl, "New Order", request);

        context.getHttpClient().executeRequest(requestBuilder)
                .toCompletableFuture()
                .handle(new GeminiResponseHandler(context.getObjectMapper(), ORDER_STATUS_TYPE, callback));
    }

    public void cancelOrder(CancelOrderRequest request, BiConsumer<OrderStatus, Throwable> callback) {
        request.setRequest("/v1/order/cancel");
        request.setNonce(context.getApiKeyContext().nextNonce());

        final RequestBuilder requestBuilder = createRequestBuilder(cancelOrderUrl, "Cancel Order", request);

        context.getHttpClient().executeRequest(requestBuilder)
                .toCompletableFuture()
                .handle(new GeminiResponseHandler(context.getObjectMapper(), ORDER_STATUS_TYPE, callback));
    }

    public void loadOrderStatus(OrderStatusRequest request, BiConsumer<OrderStatus, Throwable> callback) {
        request.setRequest("/v1/order/status");
        request.setNonce(context.getApiKeyContext().nextNonce());

        final RequestBuilder requestBuilder = createRequestBuilder(orderStatusUrl, "Load Order", request);

        context.getHttpClient().executeRequest(requestBuilder)
                .toCompletableFuture()
                .handle(new GeminiResponseHandler(context.getObjectMapper(), ORDER_STATUS_TYPE, callback));
    }

    public void sendHeartbeat() {
        final DefaultRequest request = new DefaultRequest();
        request.setRequest("/v1/heartbeat");
        request.setNonce(context.getApiKeyContext().nextNonce());

        final RequestBuilder requestBuilder = createRequestBuilder(heartbeatUrl, "Heartbeat", request);

        context.getHttpClient().executeRequest(requestBuilder)
                .toCompletableFuture()
                .handle((BiFunction<Response, Throwable, Void>) (response, throwable) -> {
                    if (throwable != null) {
                        getLog().error("Heartbeat submission failed: %s").with(throwable);
                    } else if (response.getStatusCode() != HttpConstants.ResponseStatusCodes.OK_200) {
                        getLog().error("Heartbeat submission failed: %s").with(response.getResponseBody());
                    }
                    return null;
                });
    }

    private RequestBuilder createRequestBuilder(String requestUrl, String requestPrefix, DefaultRequest request) {
        final String payload = JsonUtil.valueAsString(context.getObjectMapper(), request);
        if (getLog().isTraceEnabled()) {
            getLog().trace("%s Request: %s").with(requestPrefix).with(payload);
        }
        final RequestBuilder requestBuilder = new RequestBuilder();
        requestBuilder
                .setMethod(HttpConstants.Methods.POST)
                .setUrl(requestUrl)
                .setHeader("Content-Type", "application/json");

        GeminiUtil.setRequestPayload(context, requestBuilder, payload);
        return requestBuilder;
    }

    // region Helper Classes

    private static class GeminiResponseHandler implements BiFunction<Response, Throwable, Void> {
        private final ObjectMapper mapper;
        private final JavaType responseType;
        private final BiConsumer<OrderStatus, Throwable> callback;

        public GeminiResponseHandler(ObjectMapper mapper, JavaType responseType, BiConsumer<OrderStatus, Throwable> callback) {
            this.mapper = mapper;
            this.responseType = responseType;
            this.callback = callback;
        }

        @Override
        public Void apply(Response response, Throwable throwable) {
            if (throwable != null) {
                callback.accept(null, throwable);
            } else if (response.getStatusCode() == HttpConstants.ResponseStatusCodes.OK_200) {
                callback.accept(JsonUtil.decodeJson(mapper, response.getResponseBodyAsBytes(), responseType), null);
            } else {
                callback.accept(null, new IllegalStateException(response.getResponseBody()));
            }
            return null;
        }
    }

    // endregion
}
