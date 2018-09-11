package zav.mw.poc.http;

import static org.springframework.web.reactive.function.server.RequestPredicates.*;
import static org.springframework.web.reactive.function.server.RouterFunctions.route;
import static org.springframework.web.reactive.function.server.ServerResponse.ok;

import java.io.InputStream;
import java.security.KeyStore;
import java.security.PrivateKey;
import java.security.cert.X509Certificate;

import javax.net.ssl.SSLException;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.task.SimpleAsyncTaskExecutor;
import org.springframework.core.task.TaskExecutor;
import org.springframework.http.server.reactive.HttpHandler;
import org.springframework.http.server.reactive.ReactorHttpHandlerAdapter;
import org.springframework.web.reactive.function.BodyInserters;
import org.springframework.web.reactive.function.server.RouterFunction;
import org.springframework.web.reactive.function.server.RouterFunctions;
import org.springframework.web.reactive.function.server.ServerResponse;

import io.netty.handler.ssl.SslContext;
import io.netty.handler.ssl.SslContextBuilder;
import reactor.ipc.netty.http.server.HttpServer;

@Configuration
public class HttpWfServiceConfig {

	public static final String TEST_KAFKA_RR_ECHO_URI = "/test-kafka-rr-echo";
	public static final String JUST_LOG_URI = "/test-internal-call";

	private final Logger log = LogManager.getLogger(this.getClass());

	@Value("${server.port}")
	private int portNumber;

	@Value("${zavMwPoc.ssl.pkcs12-path}")
	private String pkcs12KeyStorePath;

	@Value("${zavMwPoc.ssl.password}")
	private String pkcs12KeyStorePassword;

	@Value("${zavMwPoc.ssl.password}")
	private String pkcs12KeyPassword;

	@Value("${zavMwPoc.ssl.alias}")
	private String pkcs12KeyStoreAlias;

	@Autowired
	private ApplicationContext applicationContext;

	@Bean
	public RouterFunction<ServerResponse> httpWfRouterFunction(HttpWfHandler httpWfHandler) {
		return route(POST("/test-kafka-send/{key}"), httpWfHandler::sendToKafka)
				.andRoute(POST(JUST_LOG_URI), httpWfHandler::justLog)
				.andRoute(POST("/test-kafka-rr/{key}"), httpWfHandler::syncRequestResponse)
				.andRoute(POST(TEST_KAFKA_RR_ECHO_URI),
						request -> ok().body(BodyInserters.fromPublisher(
								request.bodyToMono(String.class).map(message -> "echoed message: " + message),
								String.class)));
	}

	@Bean
	public ReactorHttpHandlerAdapter httpHandlerAdapter(RouterFunction<ServerResponse> rf) {
		HttpHandler httpHandler = RouterFunctions.toHttpHandler(rf);
		return new ReactorHttpHandlerAdapter(httpHandler);
	}

	@Bean
	public HttpServer httpServer(PrivateKey privateKey, X509Certificate certificate) {
		// return HttpServer.create(portNumber);
		return HttpServer.create(options -> {
			options.port(portNumber);
			try {
				SslContext sslContext = SslContextBuilder.forServer(privateKey, pkcs12KeyPassword, certificate).build();
				options.sslContext(sslContext);
			} catch (SSLException e) {
				log.error("could not create http server", e);
				throw new RuntimeException(e);
			}
		});
	}

	@Bean
	public TaskExecutor httpServerLauncher() {
		return new SimpleAsyncTaskExecutor();
	}

	@Bean
	public CommandLineRunner httpServerRunner(TaskExecutor executor, HttpServer httpServer,
			ReactorHttpHandlerAdapter adapter) {
		return args -> {
			executor.execute(() -> {
				httpServer.newHandler(adapter).block();
			});
		};
	}

	@Bean
	public PrivateKey privateKey() throws Exception {
		InputStream is = applicationContext.getResource(pkcs12KeyStorePath).getInputStream();
		KeyStore keyStore = KeyStore.getInstance("pkcs12");
		keyStore.load(is, pkcs12KeyStorePassword.toCharArray());
		is.close();
		return (PrivateKey) keyStore.getKey(pkcs12KeyStoreAlias, pkcs12KeyPassword.toCharArray());
	}

	@Bean
	public X509Certificate certificate() throws Exception {
		InputStream is = applicationContext.getResource(pkcs12KeyStorePath).getInputStream();
		KeyStore keyStore = KeyStore.getInstance("pkcs12");
		keyStore.load(is, pkcs12KeyStorePassword.toCharArray());
		is.close();
		return (X509Certificate) keyStore.getCertificate(pkcs12KeyStoreAlias);
	}
}
