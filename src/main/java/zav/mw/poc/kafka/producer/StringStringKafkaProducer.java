package zav.mw.poc.kafka.producer;

import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.common.header.internals.RecordHeader;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.kafka.support.SendResult;
import org.springframework.stereotype.Component;
import org.springframework.util.concurrent.ListenableFuture;
import org.springframework.util.concurrent.ListenableFutureCallback;

import reactor.core.publisher.Mono;

@Component
public class StringStringKafkaProducer {

	public static final String CORRELATION_ID = "correlationId";

	@Autowired
	private KafkaTemplate<String, String> kafkaStringStringTemplate;

	private final Logger log = LogManager.getLogger(this.getClass());

	@Value("${zavMwPoc.kafka.topic}")
	private String topic;

	@Value("${zavMwPoc.kafka.sync-req-topic}")
	private String syncReqTopic;

	@Value("${zavMwPoc.kafka.sync-resp-topic}")
	private String syncRespTopic;

	public Mono<String> send(String key, String message) {

		final ProducerRecord<String, String> record = new ProducerRecord<String, String>(topic, key, message);
		ListenableFuture<SendResult<String, String>> future = kafkaStringStringTemplate.send(record);

		return Mono.<String>create(monoSink -> {
			future.addCallback(new ListenableFutureCallback<SendResult<String, String>>() {

				@Override
				public void onSuccess(SendResult<String, String> result) {
					log.debug("sent message with key " + result.getProducerRecord().key());
					monoSink.success(
							result.getRecordMetadata().partition() + ":" + result.getRecordMetadata().offset());
				}

				@Override
				public void onFailure(Throwable ex) {
					log.error("could not send message to kafka", ex);
					monoSink.error(ex);
				}
			});
		});

	}

	public Mono<String> sendForSync(String correlationId, String key, String message) {
		final ProducerRecord<String, String> record = new ProducerRecord<String, String>(syncReqTopic, key, message);
		record.headers().add(new RecordHeader(KafkaHeaders.CORRELATION_ID, correlationId.getBytes()));
		record.headers().add(new RecordHeader(KafkaHeaders.REPLY_TOPIC, syncRespTopic.getBytes()));
		ListenableFuture<SendResult<String, String>> future = kafkaStringStringTemplate.send(record);

		return Mono.<String>create(monoSink -> {
			future.addCallback(new ListenableFutureCallback<SendResult<String, String>>() {

				@Override
				public void onSuccess(SendResult<String, String> result) {
					log.debug("sent message with key " + result.getProducerRecord().key() + " and correlation id " + correlationId);
					monoSink.success("sent message with correlation id " + correlationId + ", "
							+ result.getRecordMetadata().partition() + ":" + result.getRecordMetadata().offset());
				}

				@Override
				public void onFailure(Throwable ex) {
					log.error("could not send message to kafka", ex);
					monoSink.error(ex);
				}
			});
		});
	}
}
