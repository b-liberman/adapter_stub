package zav.mw.poc.kafka.producer;

import java.util.HashMap;
import java.util.Map;

import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.common.config.SaslConfigs;
import org.apache.kafka.common.serialization.StringSerializer;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.annotation.EnableKafka;
import org.springframework.kafka.core.DefaultKafkaProducerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.core.ProducerFactory;

@Configuration
@EnableKafka
public class KafkaProducerConfig {

	@Value("${zavMwPoc.kafka.bootstrapServers}")
	private String kafkaBootstrapServers;

	@Bean
	public Map<String, Object> stringStringProducerConfigs() {
		Map<String, Object> producerProps = new HashMap<>();
		producerProps.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, kafkaBootstrapServers);
		producerProps.put(ProducerConfig.CLIENT_ID_CONFIG, "ZavMwPoCProducer");
		producerProps.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class.getName());
		producerProps.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, StringSerializer.class.getName());

		producerProps.put(SaslConfigs.SASL_MECHANISM, "PLAIN");
		producerProps.put("security.protocol", "SASL_PLAINTEXT");
		producerProps.put(SaslConfigs.SASL_JAAS_CONFIG,
				"org.apache.kafka.common.security.plain.PlainLoginModule required username=\"zavpoc\" password=\"zavpoc\";");
		return producerProps;
	}

	@Bean
	public ProducerFactory<String, String> stringStringProducerFactory() {
		return new DefaultKafkaProducerFactory<String, String>(stringStringProducerConfigs());
	}

	@Bean
	public KafkaTemplate<String, String> kafkaStringStringTemplate() {
		return new KafkaTemplate<String, String>(stringStringProducerFactory());
	}

}
