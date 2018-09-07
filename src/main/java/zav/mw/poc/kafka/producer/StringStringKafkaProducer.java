package zav.mw.poc.kafka.producer;

import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;
import org.springframework.stereotype.Component;
import org.springframework.util.concurrent.ListenableFuture;
import org.springframework.util.concurrent.ListenableFutureCallback;

import reactor.core.publisher.Mono;

@Component
public class StringStringKafkaProducer {

	@Autowired
	private KafkaTemplate<String, String> kafkaStringStringTemplate;

	private final Logger log = LogManager.getLogger(this.getClass());

	@Value("${zavMwPoc.kafka.topic}")
	private String topic;

	public Mono<Void> send(String key, String message) {

		final ProducerRecord<String, String> record = new ProducerRecord<String, String>(topic, key, message);
		ListenableFuture<SendResult<String, String>> future = kafkaStringStringTemplate.send(record);

		return Mono.<Void>create(cb -> {
			future.addCallback(new ListenableFutureCallback<SendResult<String, String>>() {

				@Override
				public void onSuccess(SendResult<String, String> result) {
					log.debug("sent message with key " + result.getProducerRecord().key());
					cb.success();
				}

				@Override
				public void onFailure(Throwable ex) {
					log.error("could not send message to kafka", ex);
					cb.error(ex);
				}
			});
		});

	}
}
