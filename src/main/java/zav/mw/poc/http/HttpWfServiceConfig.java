package zav.mw.poc.http;

import static org.springframework.web.reactive.function.server.RequestPredicates.POST;
import static org.springframework.web.reactive.function.server.RouterFunctions.route;
import static org.springframework.web.reactive.function.server.ServerResponse.ok;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
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

import reactor.ipc.netty.http.server.HttpServer;

@Configuration
public class HttpWfServiceConfig {

	public static final String TEST_KAFKA_RR_ECHO_URI = "/test-kafka-rr-echo";
	public static final String JUST_LOG_URI = "/test-internal-call";

	@Value("${server.port}")
	private int portNumber;

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
	public HttpServer httpServer() {
		return HttpServer.create(portNumber);
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
}
