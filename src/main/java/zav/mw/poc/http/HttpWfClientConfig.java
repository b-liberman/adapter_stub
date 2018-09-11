package zav.mw.poc.http;

import java.security.cert.X509Certificate;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.reactive.ClientHttpConnector;
import org.springframework.http.client.reactive.ReactorClientHttpConnector;
import org.springframework.web.reactive.function.client.WebClient;

import io.netty.handler.ssl.SslContext;
import io.netty.handler.ssl.SslContextBuilder;

@Configuration
public class HttpWfClientConfig {

	@Value("${server.port}")
	private int portNumber;

	@Bean
	public WebClient webClient(X509Certificate certificate) throws Exception {

		SslContext sslContext = SslContextBuilder.forClient().trustManager(certificate).build();
		ClientHttpConnector httpsConnector = new ReactorClientHttpConnector(options -> {
			options.sslContext(sslContext);
			options.host("localhost");
			options.port(portNumber);
		});
		return WebClient.builder().baseUrl("https://localhost:" + portNumber).clientConnector(httpsConnector).build();
	}
}
