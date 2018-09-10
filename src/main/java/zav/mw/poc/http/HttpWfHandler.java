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
import reactor.core.scheduler.Schedulers;
import zav.mw.poc.kafka.producer.StringStringKafkaProducer;

@Component
public class HttpWfHandler {

	@Autowired
	private StringStringKafkaProducer producer;

	private final Logger log = LogManager.getLogger(this.getClass());

	public Mono<ServerResponse> sendToKafka(ServerRequest request) {

		String key = request.pathVariable("key") + UUID.randomUUID();

		return Mono.<ServerResponse>create(monoSink -> {

			request.body(BodyExtractors.toMono(String.class)).doOnSuccess(message -> {
				producer.send(key, message + "\n" + UUID.randomUUID()).doOnSuccess(responseFromKafka -> {
					log.debug("sent message with key " + key);
					ok().body(BodyInserters.fromObject("message sent to kafka: " + responseFromKafka))
							.doOnSuccess(serverResponse -> monoSink.success(serverResponse)).subscribe();
				}).doOnError(t -> {
					log.error("could send to kafka", t);
					monoSink.error(t);
				}).subscribeOn(Schedulers.newSingle("kafka-thread")).subscribe();
			}).doOnError(t -> {
				log.error("could not extract body", t);
				monoSink.error(t);
			}).subscribe();
		});
	}

	public Mono<ServerResponse> justLog(ServerRequest request) {

		return Mono.<ServerResponse>create(monoSink -> {
			request.body(BodyExtractors.toMono(String.class)).doOnSuccess(message -> {
				log.debug("received message: " + message);
				ok().body(BodyInserters.fromObject("just logged"))
						.doOnSuccess(serverResponse -> monoSink.success(serverResponse)).subscribe();
			}).doOnError(t -> {
				log.error("could not extract body", t);
				monoSink.error(t);
			}).subscribe();
		});
	}
}
