package zav.mw.poc.http;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.server.ServerResponse;

import reactor.core.publisher.MonoSink;

@Component
public class SyncReqRespHelper {

	private Map<String, MonoSink<ServerResponse>> pendingRequests = Collections
			.synchronizedMap(new HashMap<String, MonoSink<ServerResponse>>());

	public void addRecord(String correlationId, MonoSink<ServerResponse> ms) {
		pendingRequests.put(correlationId, ms);
	}

	public boolean containsRecord(String correlationId) {
		return pendingRequests.containsKey(correlationId);
	}

	public MonoSink<ServerResponse> retrieveRecord(String correlationId) {
		return pendingRequests.get(correlationId);
	}

	public void deleteRecord(String correlationId) {
		pendingRequests.remove(correlationId);
	}
}
