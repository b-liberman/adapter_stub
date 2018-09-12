package zav.mw.poc.kafka.consumer;

import java.util.HashMap;
import java.util.Map;

import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.common.config.SaslConfigs;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.annotation.EnableKafka;
import org.springframework.kafka.config.ConcurrentKafkaListenerContainerFactory;
import org.springframework.kafka.core.ConsumerFactory;
import org.springframework.kafka.core.DefaultKafkaConsumerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SimpleKafkaHeaderMapper;
import org.springframework.kafka.support.converter.MessagingMessageConverter;

@Configuration
@EnableKafka
public class KafkaConsumerConfig {

	@Value("${zavMwPoc.kafka.bootstrapServers}")
	private String kafkaBootstrapServers;

	@Bean
	public Map<String, Object> stringStringConsumerConfigs() {
		Map<String, Object> consumerProps = new HashMap<>();
		consumerProps.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, kafkaBootstrapServers);
		consumerProps.put(ConsumerConfig.CLIENT_ID_CONFIG, "ZavMwPoCConsumer");
		consumerProps.put(ConsumerConfig.GROUP_ID_CONFIG, "ZavMwPoCConsumer");
		consumerProps.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class.getName());
		consumerProps.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class.getName());

		consumerProps.put(SaslConfigs.SASL_MECHANISM, "PLAIN");
		consumerProps.put("security.protocol", "SASL_PLAINTEXT");
		consumerProps.put(SaslConfigs.SASL_JAAS_CONFIG,
				"org.apache.kafka.common.security.plain.PlainLoginModule required username=\"zavpoc\" password=\"zavpoc\";");

		return consumerProps;
	}

	@Bean
	public ConsumerFactory<String, String> consumerFactory() {
		return new DefaultKafkaConsumerFactory<>(stringStringConsumerConfigs());
	}

	@Bean
	public ConcurrentKafkaListenerContainerFactory<String, String> kafkaListenerContainerFactory(
			KafkaTemplate<String, String> kt) {

		ConcurrentKafkaListenerContainerFactory<String, String> factory = new ConcurrentKafkaListenerContainerFactory<>();
		factory.setConsumerFactory(consumerFactory());
		factory.setReplyTemplate(kt);
		return factory;
	}

	@Bean // not required if Jackson is on the classpath
	public MessagingMessageConverter simpleMapperConverter() {
		MessagingMessageConverter messagingMessageConverter = new MessagingMessageConverter();
		messagingMessageConverter.setHeaderMapper(new SimpleKafkaHeaderMapper());
		return messagingMessageConverter;
	}
}
