package org.migor.rich.rss.trigger

import org.migor.rich.rss.database2.models.NativeFeedEntity
import org.migor.rich.rss.database2.models.NativeFeedStatus
import org.migor.rich.rss.database2.repositories.NativeFeedDAO
import org.migor.rich.rss.harvest.FeedHarvester2
import org.migor.rich.rss.util.CryptUtil
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.context.annotation.Profile
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.util.*

@Service
@Profile("database2")
class TriggerFeed internal constructor() {

  @Autowired
  lateinit var feedRepository: NativeFeedDAO

  @Autowired
  lateinit var feedHarvester: FeedHarvester2

  @Scheduled(fixedDelay = 1234)
  @Transactional(readOnly = true)
  fun fetchFeeds() {
    val excludedStates = arrayOf(NativeFeedStatus.DEACTIVATED)
    feedRepository.findAllDueToFeeds(Date(), excludedStates)
      .forEach { feed: NativeFeedEntity ->
        feedHarvester.harvestFeed(CryptUtil.newCorrId(), feed)
      }
  }
}
