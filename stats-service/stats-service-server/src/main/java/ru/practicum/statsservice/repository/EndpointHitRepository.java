package ru.practicum.statsservice.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import ru.practicum.statsservice.dto.ViewStatsDto;
import ru.practicum.statsservice.model.EndpointHit;

import java.time.LocalDateTime;
import java.util.List;

public interface EndpointHitRepository extends JpaRepository<EndpointHit, Long> {

    @Query("select new ru.practicum.statsservice.dto.ViewStatsDto(hit.app, hit.uri, cast(count(hit.ip) as integer)) " +
            "from EndpointHit as hit " +
            "where hit.timestamp between ?1 and ?2 group by hit.app, hit.uri " +
            "order by count(hit.ip) desc")
    List<ViewStatsDto> getViewStats(LocalDateTime start, LocalDateTime end);

    @Query("select new ru.practicum.statsservice.dto.ViewStatsDto(hit.app, hit.uri, cast(count(distinct hit.ip) as integer)) " +
            "from EndpointHit as hit " +
            "where hit.timestamp between ?1 and ?2 group by hit.app, hit.uri " +
            "order by count(distinct hit.ip) desc")
    List<ViewStatsDto> getViewStatsWithUniqueIps(LocalDateTime start, LocalDateTime end);

    @Query("select new ru.practicum.statsservice.dto.ViewStatsDto(hit.app, hit.uri, cast(count(hit.ip) as integer)) " +
            "from EndpointHit as hit " +
            "where hit.timestamp between ?1 and ?2 and hit.uri in ?3 group by hit.app, hit.uri " +
            "order by count(hit.ip) desc")
    List<ViewStatsDto> getViewStatsByUriIn(LocalDateTime start, LocalDateTime end, List<String> uris);

    @Query("select new ru.practicum.statsservice.dto.ViewStatsDto(hit.app, hit.uri, cast(count(distinct hit.ip) as integer)) " +
            "from EndpointHit as hit " +
            "where hit.timestamp between ?1 and ?2 and hit.uri in ?3 group by hit.app, hit.uri " +
            "order by count(distinct hit.ip) desc")
    List<ViewStatsDto> getViewStatsByUriInWithUniqueIps(LocalDateTime start, LocalDateTime end, List<String> uris);
}
