package zav.mw.poc.kafka.consumer;

import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.BodyExtractors;
import org.springframework.web.reactive.function.BodyInserters;
import org.springframework.web.reactive.function.client.WebClient;

import reactor.core.scheduler.Schedulers;
import zav.mw.poc.http.HttpWfServiceConfig;

@Component
public class StringStringKafkaConsumer {

	private final Logger log = LogManager.getLogger(this.getClass());

	@Autowired
	private WebClient webClient;

	@KafkaListener(topics = "${zavMwPoc.kafka.topic}")
	public void listen(ConsumerRecord<String, String> record) {
		log.debug("received message with key " + record.key() + " value " + record.value());
		webClient.post().uri(HttpWfServiceConfig.TEST_INTERNAL_CALL).body(BodyInserters.fromObject(record.value()))
				.exchange()
				.doOnSuccess(cr -> cr.body(BodyExtractors.toMono(String.class))
						.subscribe(returnedMessage -> log.debug(returnedMessage)))
				.subscribeOn(Schedulers.newSingle("web-client-thread")).subscribe();
	}
}
