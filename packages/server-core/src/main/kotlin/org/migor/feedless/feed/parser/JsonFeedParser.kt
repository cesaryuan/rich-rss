package org.migor.feedless.feed.parser

import com.google.gson.GsonBuilder
import org.migor.feedless.api.dto.RichFeed
import org.migor.feedless.feed.parser.json.JsonFeed
import org.migor.feedless.harvest.HarvestResponse
import org.slf4j.LoggerFactory

class JsonFeedParser : FeedBodyParser {

  private val log = LoggerFactory.getLogger(JsonFeedParser::class.simpleName)
  private val FORMAT_RFC3339 = "yyyy-MM-dd'T'HH:mm:ss-Z"

  override fun priority(): Int {
    return 1
  }

  override fun canProcess(feedType: FeedType): Boolean {
    return feedType == FeedType.JSON
  }

  override fun process(corrId: String, response: HarvestResponse): RichFeed {
    val gson = GsonBuilder()
      .setDateFormat(FORMAT_RFC3339)
      .create()
    return RichFeed(gson.fromJson(patchResponse(response), JsonFeed::class.java))
  }

  private fun patchResponse(harvestResponse: HarvestResponse): String {
    val responseBody = String(harvestResponse.response.responseBody).trim()
    return if (responseBody.startsWith("[")) {
      "{\"items\": $responseBody}"
    } else {
      responseBody
    }
  }
}
