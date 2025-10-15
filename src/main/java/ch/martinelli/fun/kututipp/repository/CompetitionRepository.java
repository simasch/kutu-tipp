package ch.martinelli.fun.kututipp.repository;

import ch.martinelli.fun.kututipp.db.tables.records.CompetitionRecord;
import org.jooq.DSLContext;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

import static ch.martinelli.fun.kututipp.db.tables.Competition.COMPETITION;

/**
 * Repository for competition database operations using jOOQ.
 */
@Repository
public class CompetitionRepository {

    private final DSLContext dsl;

    public CompetitionRepository(DSLContext dsl) {
        this.dsl = dsl;
    }

    /**
     * Finds all competitions ordered by date descending (most recent first).
     *
     * @return List of all competitions
     */
    public List<CompetitionRecord> findAll() {
        return dsl.selectFrom(COMPETITION)
                .orderBy(COMPETITION.DATE.desc())
                .fetch();
    }

    /**
     * Finds a competition by ID.
     *
     * @param id the competition ID
     * @return an Optional containing the competition record if found, empty otherwise
     */
    public Optional<CompetitionRecord> findById(Long id) {
        return dsl.selectFrom(COMPETITION)
                .where(COMPETITION.ID.eq(id))
                .fetchOptional();
    }

    /**
     * Finds all upcoming competitions ordered by date ascending (soonest first).
     *
     * @return List of upcoming competitions
     */
    public List<CompetitionRecord> findUpcoming() {
        return dsl.selectFrom(COMPETITION)
                .where(COMPETITION.STATUS.eq(ch.martinelli.fun.kututipp.db.enums.CompetitionStatus.upcoming))
                .orderBy(COMPETITION.DATE.asc())
                .fetch();
    }
}
