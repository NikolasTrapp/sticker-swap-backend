package br.com.stickerswap.infrastructure.repository.search;

import br.com.stickerswap.api.search.dto.HolderResponse;
import br.com.stickerswap.domain.search.model.HolderSearchCriteria;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.Tuple;
import org.hibernate.Session;
import org.hibernate.query.NativeQuery;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Repository
public class HolderSearchRepositoryImpl implements HolderSearchRepository {

    // CTE-based query. Slots (in order): dist_sq expression, match predicate, exclude clause.
    private static final String DATA_SQL_TPL = """
            WITH h AS (
                SELECT
                    urs.user_id,
                    up.nickname,
                    CASE WHEN up.show_city_state_publicly THEN up.city  END AS city,
                    CASE WHEN up.show_city_state_publicly THEN up.state END AS state,
                    urs.quantity,
                    u.last_activity_at,
                    CASE WHEN up.approximate_latitude IS NULL
                              OR up.use_location_for_search = FALSE
                         THEN 1 ELSE 0 END                                             AS no_loc,
                    %s                                                                 AS dist_sq,
                    CASE WHEN %s THEN 0 ELSE 1 END                                     AS no_match,
                    CASE WHEN u.last_activity_at IS NULL THEN 1 ELSE 0 END             AS no_activity
                FROM user_repeated_stickers urs
                JOIN users u ON u.id = urs.user_id
                LEFT JOIN user_profiles up ON up.user_id = urs.user_id
                WHERE urs.album_id = :albumId
                  AND urs.sticker_id = :stickerId
                  AND urs.quantity > 0
                  %s
            )
            SELECT user_id, nickname, city, state, quantity,
                   no_match = 0 AS is_potential_match, last_activity_at
            FROM h
            ORDER BY no_loc ASC,
                     dist_sq ASC NULLS LAST,
                     no_match ASC,
                     quantity DESC,
                     no_activity ASC,
                     last_activity_at DESC NULLS LAST
            """;

    // Slot: exclude clause.
    private static final String COUNT_SQL_TPL = """
            SELECT COUNT(*)
            FROM user_repeated_stickers
            WHERE album_id = :albumId
              AND sticker_id = :stickerId
              AND quantity > 0
              %s
            """;

    private static final String DIST_SQ_EXPR =
            "(up.approximate_latitude - :lat) * (up.approximate_latitude - :lat)"
            + " + (up.approximate_longitude - :lon) * (up.approximate_longitude - :lon)";

    private static final String MATCH_EXISTS =
            "EXISTS (SELECT 1 FROM user_wanted_stickers uws"
            + " WHERE uws.user_id = urs.user_id AND uws.sticker_id IN (:searcherIds))";

    @PersistenceContext
    private EntityManager em;

    @Override
    @Transactional(readOnly = true)
    public Page<HolderResponse> findHolders(HolderSearchCriteria c, Pageable pageable) {
        boolean hasLoc = c.searcherLat() != null && c.searcherLon() != null;
        boolean hasExcluded = !c.excludedUserIds().isEmpty();
        boolean hasSearcherStickers = !c.searcherRepeatedStickerIds().isEmpty();

        Session session = em.unwrap(Session.class);

        long total = count(session, c, hasExcluded);
        if (total == 0 || pageable.getOffset() >= total) {
            return Page.empty(pageable);
        }

        List<HolderResponse> content = fetchPage(
                session, c, pageable, hasLoc, hasExcluded, hasSearcherStickers);

        return new PageImpl<>(content, pageable, total);
    }

    private long count(Session session, HolderSearchCriteria c, boolean hasExcluded) {
        String excludeClause = hasExcluded ? "AND user_id NOT IN (:excludedIds)" : "";
        String sql = String.format(COUNT_SQL_TPL, excludeClause);

        NativeQuery<Long> q = session.createNativeQuery(sql, Long.class);
        q.setParameter("albumId", c.albumId());
        q.setParameter("stickerId", c.stickerId());
        if (hasExcluded) {
            q.setParameterList("excludedIds", c.excludedUserIds());
        }
        return q.getSingleResult();
    }

    private List<HolderResponse> fetchPage(Session session, HolderSearchCriteria c,
                                            Pageable pageable,
                                            boolean hasLoc, boolean hasExcluded, boolean hasSearcherStickers) {
        String distSq = hasLoc ? DIST_SQ_EXPR : "NULL";
        String matchPredicate = hasSearcherStickers ? MATCH_EXISTS : "FALSE";
        String excludeClause = hasExcluded ? "AND urs.user_id NOT IN (:excludedIds)" : "";

        String sql = String.format(DATA_SQL_TPL, distSq, matchPredicate, excludeClause);

        NativeQuery<Tuple> q = session.createNativeQuery(sql, Tuple.class);
        q.setParameter("albumId", c.albumId());
        q.setParameter("stickerId", c.stickerId());
        if (hasLoc) {
            q.setParameter("lat", c.searcherLat());
            q.setParameter("lon", c.searcherLon());
        }
        if (hasExcluded) {
            q.setParameterList("excludedIds", c.excludedUserIds());
        }
        if (hasSearcherStickers) {
            q.setParameterList("searcherIds", c.searcherRepeatedStickerIds());
        }

        q.setFirstResult((int) pageable.getOffset());
        q.setMaxResults(pageable.getPageSize());

        return q.getResultList().stream().map(this::mapTuple).toList();
    }

    private HolderResponse mapTuple(Tuple t) {
        UUID userId = t.get("user_id", UUID.class);
        String nickname = t.get("nickname", String.class);
        String city = t.get("city", String.class);
        String state = t.get("state", String.class);
        int quantity = ((Number) t.get("quantity")).intValue();
        boolean isPotentialMatch = (Boolean) t.get("is_potential_match");
        LocalDateTime lastActivityAt = toLocalDateTime(t.get("last_activity_at"));
        return new HolderResponse(userId, nickname, city, state, quantity, isPotentialMatch, lastActivityAt);
    }

    private LocalDateTime toLocalDateTime(Object val) {
        if (val == null) return null;
        if (val instanceof LocalDateTime ldt) return ldt;
        if (val instanceof java.sql.Timestamp ts) return ts.toLocalDateTime();
        return null;
    }
}
