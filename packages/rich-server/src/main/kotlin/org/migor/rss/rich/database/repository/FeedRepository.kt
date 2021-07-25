package org.migor.rss.rich.database.repository

import org.migor.rss.rich.database.enums.FeedStatus
import org.migor.rss.rich.database.model.Feed
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.Modifying
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.PagingAndSortingRepository
import org.springframework.data.repository.query.Param
import org.springframework.stereotype.Repository
import java.util.*
import javax.transaction.Transactional


@Repository
interface FeedRepository : PagingAndSortingRepository<Feed, String> {

  @Query("""select distinct f from Feed f
    where (f.nextHarvestAt < :now or f.nextHarvestAt is null ) and f.status not in :states""")
  fun findAllByNextHarvestAtIsBeforeAndStatusEquals(@Param("now") now: Date, @Param("states") states: Array<FeedStatus>, pageable: Pageable): List<Feed>

  @Transactional
  @Modifying
  @Query("update Feed s set s.nextHarvestAt = :nextHarvestAt, s.harvestIntervalMinutes = :harvestInterval where s.id = :id")
  fun updateNextHarvestAtAndHarvestInterval(@Param("id") sourceId: String,
                                            @Param("nextHarvestAt") nextHarvestAt: Date,
                                            @Param("harvestInterval") harvestInterval: Int)

  @Transactional
  @Modifying
  @Query("update Feed f set f.updatedAt = :updatedAt where f.id = :id")
  fun updateUpdatedAt(@Param("id") feedId: String, @Param("updatedAt") updatedAt: Date)

  @Modifying
  @Query("update Feed s set s.status = :status where s.id = :id")
  fun updateStatus(@Param("id") sourceId: String,
                   @Param("status") status: FeedStatus)

  fun findByStreamId(streamId: String): Feed
}