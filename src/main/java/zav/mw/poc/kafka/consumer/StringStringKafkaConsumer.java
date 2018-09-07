package zav.mw.poc.kafka.consumer;

import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Component
public class StringStringKafkaConsumer {

	private final Logger log = LogManager.getLogger(this.getClass());

	@KafkaListener(topics = "${zavMwPoc.kafka.topic}")
	public void listen(ConsumerRecord<String, String> record) {
		log.debug("received message with key " + record.key() + " value " + record.value());
	}
}
