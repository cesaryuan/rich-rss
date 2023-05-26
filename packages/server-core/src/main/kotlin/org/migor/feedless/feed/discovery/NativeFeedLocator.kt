package org.migor.feedless.feed.discovery

import org.apache.commons.lang3.StringUtils
import org.jsoup.nodes.Document
import org.jsoup.nodes.Element
import org.migor.feedless.service.FeedService.Companion.absUrl
import org.migor.feedless.util.FeedUtil
import org.springframework.stereotype.Service

@Service
class NativeFeedLocator {

  fun locateInDocument(document: Document, url: String): List<TransientOrExistingNativeFeed> {
    return document.select("link[rel=alternate][type], link[rel=feed][type]")
      .mapIndexedNotNull { index, element -> toFeedReference(index, element, url) }
      .distinctBy { it.transient?.url }
  }

  private fun toFeedReference(index: Int, element: Element, url: String): TransientOrExistingNativeFeed? {
    try {
      return TransientOrExistingNativeFeed(transient = TransientNativeFeed(
          absUrl(url, element.attr("href")),
          FeedUtil.detectFeedType(element.attr("type")),
          StringUtils.trimToNull(element.attr("title")) ?: "Native Feed #${index+1}"
        )
      )
    } catch (e: Exception) {
      return null
    }
  }
}
