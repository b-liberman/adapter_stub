package zav.mw.poc.kafka.consumer;

import static org.springframework.web.reactive.function.server.ServerResponse.ok;

import java.util.concurrent.CompletableFuture;

import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.messaging.handler.annotation.SendTo;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.BodyExtractors;
import org.springframework.web.reactive.function.BodyInserters;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.server.ServerResponse;

import reactor.core.publisher.MonoSink;
import reactor.core.scheduler.Schedulers;
import zav.mw.poc.http.HttpWfServiceConfig;
import zav.mw.poc.http.SyncReqRespHelper;

@Component
public class StringStringKafkaConsumer {

	private final Logger log = LogManager.getLogger(this.getClass());

	@Autowired
	private WebClient webClient;

	@Autowired
	private SyncReqRespHelper syncReqRespHelper;

	@KafkaListener(topics = "${zavMwPoc.kafka.topic}")
	public void listen(ConsumerRecord<String, String> record) {
		webClient.post().uri(HttpWfServiceConfig.JUST_LOG_URI).body(BodyInserters.fromObject(record.value())).exchange()
				.doOnSuccess(cr -> cr.body(BodyExtractors.toMono(String.class))
						.doOnSuccess(returnedMessage -> log.debug(returnedMessage)).doOnError(t -> log.error(t))
						.subscribe())
				.doOnError(t -> log.error(t)).subscribeOn(Schedulers.newSingle("web-client-thread")).subscribe();
	}

	@KafkaListener(topics = "${zavMwPoc.kafka.sync-req-topic}")
	@SendTo
	public String listenSyncReq(ConsumerRecord<String, String> record) {

		log.debug("received sync request message with key " + record.key() + " value " + record.value());

		CompletableFuture<String> completableFuture = new CompletableFuture<String>();

		webClient.post().uri(HttpWfServiceConfig.TEST_KAFKA_RR_ECHO_URI).body(BodyInserters.fromObject(record.value()))
				.exchange()
				.doOnSuccess(cr -> cr.body(BodyExtractors.toMono(String.class))
						.doOnSuccess(returnedMessage -> completableFuture.complete(returnedMessage))
						.doOnError(t -> completableFuture.completeExceptionally(t)).subscribe())
				.doOnError(t -> completableFuture.completeExceptionally(t))
				.subscribeOn(Schedulers.newSingle("web-client-thread")).subscribe();
		try {
			// @SendTo is synchronous. Maybe we should not use it at all...
			return completableFuture.get();
		} catch (Exception e) {
			log.error("coult not retrieve sync response form the http service", e);
			return e.getMessage();
		}
	}

	@KafkaListener(topics = "${zavMwPoc.kafka.sync-resp-topic}")
	public void listenSyncResp(ConsumerRecord<String, String> record) {
		String correlationId = new String(
				record.headers().headers(KafkaHeaders.CORRELATION_ID).iterator().next().value());
		log.debug("received sync response message with key " + record.key() + " value " + record.value()
				+ " correlation id " + correlationId);
		if (!syncReqRespHelper.containsRecord(correlationId)) {
			log.debug("no record found for correlation id " + correlationId);
			return;
		}

		MonoSink<ServerResponse> monoSink = syncReqRespHelper.retrieveRecord(correlationId);
		syncReqRespHelper.deleteRecord(correlationId);

		ok().body(BodyInserters.fromObject(record.value()))
				.doOnSuccess(serverResponse -> monoSink.success(serverResponse)).doOnError(t -> monoSink.error(t))
				.subscribe();
	}
}
