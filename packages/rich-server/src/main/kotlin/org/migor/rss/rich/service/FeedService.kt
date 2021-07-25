package org.migor.rss.rich.service

import org.migor.rss.rich.api.dto.FeedJsonDto
import org.migor.rss.rich.database.enums.FeedStatus
import org.migor.rss.rich.database.model.Feed
import org.migor.rss.rich.database.model.FeedEvent
import org.migor.rss.rich.database.repository.ArticleRepository
import org.migor.rss.rich.database.repository.FeedEventRepository
import org.migor.rss.rich.database.repository.FeedRepository
import org.migor.rss.rich.harvest.FeedData
import org.migor.rss.rich.harvest.HarvestException
import org.migor.rss.rich.harvest.HarvestResponse
import org.migor.rss.rich.harvest.feedparser.JsonFeedParser
import org.migor.rss.rich.harvest.feedparser.XmlFeedParser
import org.migor.rss.rich.util.FeedUtil
import org.migor.rss.rich.util.HttpUtil
import org.migor.rss.rich.util.JsonUtil
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.data.domain.PageRequest
import org.springframework.stereotype.Service
import java.net.ConnectException
import java.time.Duration
import java.time.temporal.ChronoUnit
import java.util.*
import javax.transaction.Transactional

@Service
class FeedService {

  private val log = LoggerFactory.getLogger(FeedService::class.simpleName)

  @Autowired
  lateinit var feedRepository: FeedRepository

  @Autowired
  lateinit var feedEventRepository: FeedEventRepository

  @Autowired
  lateinit var articleRepository: ArticleRepository

  @Autowired
  lateinit var propertyService: PropertyService

  fun parseFeed(url: String): FeedData {
    log.info("Fetching $url")
    val request = HttpUtil.client.prepareGet(url).execute()

    val response = try {
      request.get()
    } catch (e: ConnectException) {
      throw HarvestException("Cannot connect to $url cause ${e.message}")
    }
    if (response.statusCode != 200) {
      throw HarvestException("Expected 200 received ${response.statusCode}")
    }

    val harvestResponse = HarvestResponse(url, response)
    return when (FeedUtil.simpleContentType(response)) {
      "application/json" -> JsonFeedParser().process(harvestResponse)
      "application/rss+xml", "application/atom+xml", "text/xml", "application/xml" -> XmlFeedParser().process(harvestResponse)
      else -> throw HarvestException("Cannot parse contentType ${response.contentType}")
    }
  }

  fun updateUpdatedAt(source: Feed) {
    feedRepository.updateUpdatedAt(source.id!!, Date())
  }

  @Transactional
  fun updateNextHarvestDateAfterError(feed: Feed, e: Exception) {
    val nextHarvestAt = Date.from(Date().toInstant().plus(Duration.of(2, ChronoUnit.MINUTES)))
    log.info("Rescheduling failed harvest ${feed.id} to $nextHarvestAt")

    val message = Optional.ofNullable(e.message).orElse(e.javaClass.toString())
    val json = JsonUtil.gson.toJson(message)
    feedEventRepository.save(FeedEvent(json, feed, true))

    val twoWeeksAgo = Date.from(Date().toInstant().minus(Duration.of(2, ChronoUnit.HOURS))) // todo mag externalize
    feedEventRepository.deleteAllByFeedIdAndCreatedAtBefore(feed.id!!, twoWeeksAgo)

    if (feedEventRepository.countByFeedIdAndCreatedAtAfterOrderByCreatedAtDesc(feed.id!!, twoWeeksAgo) >= 5) {
      feed.status = FeedStatus.stopped
      log.info("Stopping harvest of source ${feed.id}")
    } else {
      feed.status = FeedStatus.errornous
    }
    feed.nextHarvestAt = nextHarvestAt

    feedRepository.save(feed)
  }

  fun updateNextHarvestDate(feed: Feed, hasNewEntries: Boolean) {
    val harvestIntervalMinutes = Optional.ofNullable(feed.harvestIntervalMinutes).orElse(10)
    val harvestInterval = if (hasNewEntries) {
      (harvestIntervalMinutes * 0.5).coerceAtLeast(10.0)
    } else {
      (harvestIntervalMinutes * 4).coerceAtMost(700) // twice a day
    }
//  todo mag https://developer.mozilla.org/en-US/docs/Web/HTTP/Headers/Retry-After
//    val retryAfter = responses.map { response -> response.response.getHeaders("Retry-After") }
//      .filter { retryAfter -> !retryAfter.isEmpty() }
//    slow down fetching if no content, until once a day

    val nextHarvestAt = Date.from(Date().toInstant().plus(Duration.of(harvestInterval.toLong(), ChronoUnit.MINUTES)))
    log.info("Scheduling next harvest for ${feed.feedUrl} to $nextHarvestAt")

    feedRepository.updateNextHarvestAtAndHarvestInterval(feed.id!!, nextHarvestAt, harvestInterval.toInt())
  }

  @Transactional
  fun redeemStatus(source: Feed) {
    feedRepository.updateStatus(source.id!!, FeedStatus.ok)
  }

  @Transactional
  fun findByStreamId(streamId: String): FeedJsonDto {
    val feed = feedRepository.findByStreamId(streamId)

    val pageable = PageRequest.of(0, 5)

    val articles = articleRepository.findAllByStreamId(streamId, pageable)

    return FeedJsonDto(
      id = null,
      name = feed.title!!,
      description = feed.description!!,
      home_page_url = feed.homePageUrl!!,
      date_published = feed.updatedAt!!,
      items = articles.map { article -> article.toDto() },
      feed_url = "${propertyService.host()}/stream:${streamId}",
      expired = false
    )
  }
}
