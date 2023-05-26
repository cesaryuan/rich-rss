package org.migor.feedless.feed.parser

import org.migor.feedless.api.dto.RichFeed
import org.migor.feedless.harvest.HarvestException
import org.migor.feedless.harvest.HarvestResponse

class NullFeedParser : FeedBodyParser {

  override fun priority(): Int {
    return 0
  }

  override fun canProcess(feedType: FeedType): Boolean {
    return true
  }

  override fun process(corrId: String, response: HarvestResponse): RichFeed {
    throw HarvestException("No parser found for ${response.response.contentType}")
  }
}
