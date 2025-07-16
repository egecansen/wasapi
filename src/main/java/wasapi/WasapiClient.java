package wasapi;

import context.ContextStore;
import okhttp3.Headers;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.logging.HttpLoggingInterceptor;
import okio.Buffer;
import retrofit2.Retrofit;
import retrofit2.adapter.rxjava.RxJavaCallAdapterFactory;
import retrofit2.converter.gson.GsonConverterFactory;
import retrofit2.converter.jackson.JacksonConverterFactory;
import retrofit2.converter.moshi.MoshiConverterFactory;
import retrofit2.converter.protobuf.ProtoConverterFactory;
import retrofit2.converter.scalars.ScalarsConverterFactory;
import retrofit2.converter.wire.WireConverterFactory;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.Proxy;
import java.util.Objects;
import java.util.concurrent.TimeUnit;
import utils.Printer;
import utils.reflection.ReflectionUtilities;

import static java.nio.charset.StandardCharsets.UTF_8;
import static utils.mapping.MappingUtilities.Json.getJsonString;
import static utils.mapping.MappingUtilities.Json.mapper;

/**
 * A utility class for generating Wasapi (Retrofit) service instances with flexible HTTP client configurations.
 * <p>
 * The {@code wasapi.WasapiClient} class simplifies the creation of Retrofit services by allowing the user
 * to customize headers, timeouts, logging options, proxy settings, and base URLs. It includes a default
 * {@link OkHttpClient} setup with interceptors for request logging, header injection, and optional hostname
 * verification or proxy usage.
 * </p>
 * <p>
 * This class is particularly suited for API testing and integration scenarios where detailed request
 * inspection, dynamic service generation, and configurable timeouts are critical.
 * </p>
 *
 * <h2>Key Features:</h2>
 * <ul>
 *     <li>Automatic detection of {@code BASE_URL} from service classes using reflection.</li>
 *     <li>Support for multiple converter factories (Gson, Jackson, Moshi, Protobuf, etc.).</li>
 *     <li>Customizable logging of headers and request bodies.</li>
 *     <li>Built-in support for proxy and redirect handling.</li>
 *     <li>Convenience methods for service generation.</li>
 * </ul>
 *
 * <p>
 * Example usage:
 * <pre>
 *     MyApi api = wasapi.WasapiClient.Builder()
 *     .baseUrl("https://api.example.com")
 *     .build(MyApi.class);
 * </pre>
 * <p>
 *
 * @author Umut Ay Bora, Egecan Sen
 * @version 0.0.1 (Documented in 0.0.1, migrated from another (Java-Utilities) library)
 */

@SuppressWarnings({"unused", "UnusedReturnValue"})
public class WasapiClient {

    OkHttpClient client;

    /**
     * The header object containing the headers to be added to the requests.
     */
    Headers headers = new Headers.Builder().build();

    /**
     * A boolean indicating whether to log the headers in the requests.
     */
    boolean logHeaders = Boolean.parseBoolean(ContextStore.get("log-headers", "true"));

    /**
     * A boolean indicating whether to log detailed information in the requests.
     */
    boolean detailedLogging = Boolean.parseBoolean(ContextStore.get("detailed-logging", "false"));

    /**
     * A boolean indicating whether to verify the hostname in the requests.
     */
    boolean hostnameVerification = Boolean.parseBoolean(ContextStore.get("verify-hostname", "true"));

    /**
     * A boolean indicating whether to log request body in the outgoing requests.
     */
    boolean logRequestBody = Boolean.parseBoolean(ContextStore.get("log-request-body", "false"));

    /**
     * Connection timeout in seconds.
     */
    int connectionTimeout = Integer.parseInt(ContextStore.get("connection-timeout", "60"));

    /**
     * Read timeout in seconds.
     */
    int readTimeout = Integer.parseInt(ContextStore.get("connection-read-timeout", "30"));

    /**
     * Write timeout in seconds.
     */
    int writeTimeout = Integer.parseInt(ContextStore.get("connection-write-timeout", "30"));

    /**
     * Proxy host. (default: null)
     */
    String proxyHost = ContextStore.get("proxy-host", null);

    /**
     * Proxy port (default: 8888)
     */
    int proxyPort = Integer.parseInt(ContextStore.get("proxy-port", "8888"));

    /**
     * Follow redirects?
     */
    boolean followRedirects = Boolean.parseBoolean(ContextStore.get("request-follows-redirects", "false"));

    /**
     * Use proxy?
     */
    boolean useProxy = proxyHost != null;

    /**
     * The base URL for the service.
     */
    String BASE_URL = "";

    /**
     * The logger object for logging information.
     */
    private static final Printer log = new Printer(WasapiClient.class);

    /**
     * Creates Retrofit Service based on the provided service class and configurations.
     *
     * @param serviceClass The service class (api data store) to be used when creating Retrofit Service.
     * @return The created Retrofit Service.
     */
    private <S> S generate(Class<S> serviceClass) {

        if (BASE_URL.isEmpty()) BASE_URL = (String) ReflectionUtilities.getFieldValue("BASE_URL", serviceClass);

        client = client == null ? getDefaultHttpClient() : client;

        assert BASE_URL != null;
        Retrofit retrofit = new Retrofit.Builder()
                .baseUrl(BASE_URL)
                .addConverterFactory(JacksonConverterFactory.create(mapper))
                .addConverterFactory(GsonConverterFactory.create())
                .addCallAdapterFactory(RxJavaCallAdapterFactory.create())
                .addConverterFactory(ScalarsConverterFactory.create())
                .addConverterFactory(MoshiConverterFactory.create())
                .addConverterFactory(WireConverterFactory.create())
                .addConverterFactory(ProtoConverterFactory.create())
                .client(client)
                .build();
        return retrofit.create(serviceClass);
    }

    /**
     * Creates and returns a default OkHttpClient instance with predefined configurations.
     * <p>
     * This client includes:
     * <ul>
     *     <li>Logging interceptors for both body and headers.</li>
     *     <li>Connection, read, and write timeouts.</li>
     *     <li>Redirect handling.</li>
     *     <li>A network interceptor for modifying requests before execution.</li>
     * </ul>
     * The interceptor ensures headers are set, logs the request body if enabled,
     * and logs headers when required.
     *
     * @return a configured OkHttpClient instance
     */
    private OkHttpClient getDefaultHttpClient(){
        OkHttpClient client =  new OkHttpClient.Builder()
                .connectTimeout(connectionTimeout, TimeUnit.SECONDS)
                .readTimeout(readTimeout, TimeUnit.SECONDS)
                .writeTimeout(writeTimeout, TimeUnit.SECONDS)
                .followRedirects(followRedirects)
                .addNetworkInterceptor(chain -> {
                    Request request = chain.request().newBuilder().build();
                    request = request.newBuilder()
                            .header("Host", request.url().host())
                            .method(request.method(), request.body())
                            .build();
                    for (String header: headers.names()) {
                        if (!request.headers().names().contains(header)){
                            request = request.newBuilder()
                                    .addHeader(header, Objects.requireNonNull(headers.get(header)))
                                    .build();
                        }
                    }
                    if (request.body() != null) {
                        Boolean contentLength = Objects.requireNonNull(request.body()).contentLength()!=0;
                        Boolean contentType = Objects.requireNonNull(request.body()).contentType() != null;

                        if (contentLength && contentType)
                            request = request.newBuilder()
                                    .header(
                                            "Content-Length",
                                            String.valueOf(Objects.requireNonNull(request.body()).contentLength()))
                                    .header(
                                            "Content-Type",
                                            String.valueOf(Objects.requireNonNull(request.body()).contentType()))
                                    .build();

                        if (logRequestBody) {
                            Request cloneRequest = request.newBuilder().build();
                            if (cloneRequest.body()!= null){
                                Buffer buffer = new Buffer();
                                cloneRequest.body().writeTo(buffer);
                                String bodyString = buffer.readString(UTF_8);
                                try {
                                    Object jsonObject = mapper.readValue(bodyString, Object.class);
                                    String outgoingRequestLog = "The request body is: \n" + getJsonString(jsonObject);
                                    log.info(outgoingRequestLog);
                                }
                                catch (IOException ignored) {
                                    log.warning("Could not log request body!\nBody: " + bodyString);
                                }
                            }
                            else log.warning("Request body is null!");
                        }
                    }
                    if (logHeaders)
                        log.info(("Headers(" + request.headers().size() + "): \n" + request.headers()).trim());
                    return chain.proceed(request);
                }).build();

        if (detailedLogging)
            client = new OkHttpClient.Builder(client)
                    .addInterceptor(getLogginInterceptor(HttpLoggingInterceptor.Level.BODY))
                    .addInterceptor(getLogginInterceptor(HttpLoggingInterceptor.Level.HEADERS))
                    .build();

        if (!hostnameVerification)
            client = new OkHttpClient.Builder(client)
                    .hostnameVerifier((hostname, session) -> true)
                    .build();

        if (useProxy)
            client = new OkHttpClient.Builder(client)
                    .proxy(new Proxy(Proxy.Type.HTTP, new InetSocketAddress(proxyHost, proxyPort)))
                    .build();

        return client;
    }

    /**
     * Creates and returns an {@link HttpLoggingInterceptor} with the specified logging level.
     * <p>
     * This interceptor is used to log HTTP request and response details,
     * such as headers, body, and metadata, depending on the provided level.
     *
     * @param level the logging level to set for the interceptor (e.g., BODY, HEADERS, BASIC, NONE)
     * @return an {@link HttpLoggingInterceptor} instance configured with the specified level
     */
    private HttpLoggingInterceptor getLogginInterceptor(HttpLoggingInterceptor.Level level){
        HttpLoggingInterceptor interceptor = new HttpLoggingInterceptor();
        interceptor.setLevel(level);
        return interceptor;
    }

    public static class Builder {
        private final WasapiClient generator;

        /**
         * Initializes a new Builder with default configuration values from ContextStore.
         */
        public Builder() {
            generator = new WasapiClient();
            generator.logHeaders = Boolean.parseBoolean(ContextStore.get("log-headers", "true"));
            generator.detailedLogging = Boolean.parseBoolean(ContextStore.get("detailed-logging", "false"));
            generator.hostnameVerification = Boolean.parseBoolean(ContextStore.get("verify-hostname", "true"));
            generator.logRequestBody = Boolean.parseBoolean(ContextStore.get("log-request-body", "false"));
            generator.connectionTimeout = Integer.parseInt(ContextStore.get("connection-timeout", "60"));
            generator.readTimeout = Integer.parseInt(ContextStore.get("connection-read-timeout", "30"));
            generator.writeTimeout = Integer.parseInt(ContextStore.get("connection-write-timeout", "30"));
            generator.proxyHost = ContextStore.get("proxy-host", null);
            generator.proxyPort = Integer.parseInt(ContextStore.get("proxy-port", "8888"));
            generator.useProxy = generator.proxyHost != null;
            generator.followRedirects = Boolean.parseBoolean(ContextStore.get("request-follows-redirects", "false"));
        }

        /**
         * Sets the base URL for the Retrofit client.
         */
        public Builder baseUrl(String baseUrl) {
            generator.BASE_URL = baseUrl;
            return this;
        }

        /**
         * Sets headers to be applied to all HTTP requests.
         */
        public Builder headers(Headers headers) {
            generator.headers = headers;
            return this;
        }

        /**
         * Enables or disables logging of request/response headers.
         */
        public Builder logHeaders(boolean enabled) {
            generator.logHeaders = enabled;
            return this;
        }

        /**
         * Enables or disables logging of request body.
         */
        public Builder logRequestBody(boolean enabled) {
            generator.logRequestBody = enabled;
            return this;
        }

        /**
         * Enables or disables hostname verification.
         */
        public Builder verifyHostname(boolean enabled) {
            generator.hostnameVerification = enabled;
            return this;
        }

        /**
         * Enables or disables detailed request/response logging.
         */
        public Builder detailedLogging(boolean enabled) {
            generator.detailedLogging = enabled;
            return this;
        }

        /**
         * Sets the HTTP connection timeout in seconds.
         */
        public Builder connectionTimeout(int seconds) {
            generator.connectionTimeout = seconds;
            return this;
        }

        /**
         * Sets the HTTP read timeout in seconds.
         */
        public Builder readTimeout(int seconds) {
            generator.readTimeout = seconds;
            return this;
        }

        /**
         * Sets the HTTP write timeout in seconds.
         */
        public Builder writeTimeout(int seconds) {
            generator.writeTimeout = seconds;
            return this;
        }

        /**
         * Sets proxy host and port.
         */
        public Builder proxy(String host, int port) {
            generator.proxyHost = host;
            generator.proxyPort = port;
            generator.useProxy = host != null;
            return this;
        }

        /**
         * Enables or disables automatic redirection following.
         */
        public Builder followRedirects(boolean follow) {
            generator.followRedirects = follow;
            return this;
        }

        /**
         * Sets a custom OkHttp client.
         */
        public Builder httpClient(OkHttpClient client) {
            generator.client = client;
            return this;
        }

        /**
         * Builds the service interface directly using the configured wasapi.ServiceGenerator.
         */
        public <S> S build(Class<S> serviceClass) {
            return generator.generate(serviceClass);
        }
    }
}