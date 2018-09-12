package zav.mw.poc;

import java.io.InputStream;
import java.security.KeyStore;
import java.security.cert.X509Certificate;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.boot.SpringApplication;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.http.client.reactive.ClientHttpConnector;
import org.springframework.http.client.reactive.ReactorClientHttpConnector;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.test.context.support.AnnotationConfigContextLoader;
import org.springframework.test.web.reactive.server.WebTestClient;
import org.springframework.web.reactive.function.BodyInserters;

import io.netty.handler.ssl.SslContext;
import io.netty.handler.ssl.SslContextBuilder;

@RunWith(SpringRunner.class)
@ContextConfiguration(loader = AnnotationConfigContextLoader.class)
public class SbAppTest {

	private static WebTestClient client;
	private static ConfigurableApplicationContext context;

	@BeforeClass
	public static void setup() throws Exception {
		context = SpringApplication.run(SBApplication.class);

		InputStream is = context.getResource("ssl/zavpoc.pkcs12").getInputStream();
		KeyStore keyStore = KeyStore.getInstance("pkcs12");
		keyStore.load(is, "zavpoc".toCharArray());
		is.close();
		X509Certificate certificate = (X509Certificate) keyStore.getCertificate("zavpoc");

		SslContext sslContext = SslContextBuilder.forClient().trustManager(certificate).build();
		ClientHttpConnector httpsConnector = new ReactorClientHttpConnector(options -> {
			options.sslContext(sslContext);
			options.host("localhost");
			options.port(8083);
		});
		client = WebTestClient.bindToServer(httpsConnector).baseUrl("https://localhost:8083").build();
	}

	@AfterClass
	public static void destroy() {
		SpringApplication.exit(context);
	}

	@Test
	public void testKafkaSend() throws Exception {
		client.post().uri("/test-kafka-send/abc231").body(BodyInserters.fromObject("message to kafka")).exchange()
				.expectStatus().isOk();
	}

	@Test
	public void testKafkaRr() throws Exception {
		client.post().uri("/test-kafka-rr/rr123").body(BodyInserters.fromObject("sync request")).exchange()
				.expectStatus().isOk();
	}

}
