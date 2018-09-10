package zav.mw.poc;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.springframework.boot.SpringApplication;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.test.web.reactive.server.WebTestClient;
import org.springframework.web.reactive.function.BodyInserters;

public class SbAppTest {

	private static WebTestClient client;
	private static ConfigurableApplicationContext context;

	@BeforeClass
	public static void setup() throws Exception {
		context = SpringApplication.run(SBApplication.class);
		client = WebTestClient.bindToServer().baseUrl("http://localhost:8083").build();
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
