package zav.mw.poc.http;

import static org.springframework.web.reactive.function.server.ServerResponse.ok;

import java.util.UUID;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.BodyExtractors;
import org.springframework.web.reactive.function.BodyInserters;
import org.springframework.web.reactive.function.server.ServerRequest;
import org.springframework.web.reactive.function.server.ServerResponse;

import reactor.core.publisher.Mono;
import zav.mw.poc.kafka.producer.StringStringKafkaProducer;

@Component
public class HttpWfHandler {

	@Autowired
	private StringStringKafkaProducer producer;

	private final Logger log = LogManager.getLogger(this.getClass());

	public Mono<ServerResponse> getResponse(ServerRequest request) {
		return ok().body(BodyInserters.fromObject("Hello, here I am: " + request.pathVariable("id")));
	}

	public Mono<ServerResponse> sendToKafka(ServerRequest request) {

		String key = request.pathVariable("key") + UUID.randomUUID();

		return Mono.<ServerResponse>create(cb -> {

			request.body(BodyExtractors.toMono(String.class)).doOnSuccess(message -> {
				producer.send(key, message + "\n" + UUID.randomUUID()).doOnSuccess(c -> {
					log.debug("sent message with key " + key);
					ok().body(BodyInserters.fromObject("message sent to kafka"))
							.doOnSuccess(serverResponse -> cb.success(serverResponse)).subscribe();
				}).doOnError(t -> {
					log.error("could send to kafka", t);
					cb.error(t);
				}).subscribe();
			}).doOnError(t -> {
				log.error("could not extract body", t);
				cb.error(t);
			}).subscribe();
		});
	}

	public Mono<ServerResponse> justLog(ServerRequest request) {

		return Mono.<ServerResponse>create(cb -> {
			request.body(BodyExtractors.toMono(String.class)).doOnSuccess(message -> {
				log.debug("received message: " + message);
				ok().body(BodyInserters.fromObject("just logged"))
						.doOnSuccess(serverResponse -> cb.success(serverResponse)).subscribe();
			}).doOnError(t -> {
				log.error("could not extract body", t);
				cb.error(t);
			}).subscribe();
		});
	}
}
