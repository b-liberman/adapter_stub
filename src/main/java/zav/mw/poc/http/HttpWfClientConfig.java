package zav.mw.poc.http;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.reactive.function.client.WebClient;

@Configuration
public class HttpWfClientConfig {

	@Value("${server.port}")
	private int portNumber;

	@Bean
	public WebClient webClient() {
		return WebClient.builder().baseUrl("http://localhost:" + portNumber).build();
	}
}
