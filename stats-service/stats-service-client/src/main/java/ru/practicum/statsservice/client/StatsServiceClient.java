package ru.practicum.statsservice.client;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.http.ResponseEntity;
import org.springframework.http.client.HttpComponentsClientHttpRequestFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.util.DefaultUriBuilderFactory;
import ru.practicum.statsservice.dto.EndpointHitDto;

import java.util.List;
import java.util.Map;

@Service
public class StatsServiceClient extends BaseClient {

    @Autowired
    public StatsServiceClient(@Value("${stats-service-server.url}") String serverUrl, RestTemplateBuilder builder) {
        super(
                builder
                        .uriTemplateHandler(new DefaultUriBuilderFactory(serverUrl))
                        .requestFactory(HttpComponentsClientHttpRequestFactory::new)
                        .build()
        );
    }

    public void create(EndpointHitDto dto) {
        post("/hit", dto);
    }

    public ResponseEntity<Object> getStats(String start, String end, List<String> uris, Boolean unique) {
        StringBuilder builder = new StringBuilder();
        if (uris.size() >= 1) {
            builder.append(uris.get(0));
        }
        if (uris.size() > 1) {
            for (int i = 1; i < uris.size(); i++) {
                builder.append(",").append(uris.get(i));
            }
        }
        String uri = builder.toString();
        Map<String, Object> parameters = Map.of(
                "start", start,
                "end", end,
                "uris", uri,
                "unique", unique.toString()
        );
        return get("/stats?start={start}&end={end}&uris={uris}&unique={unique}", parameters);
    }
}
