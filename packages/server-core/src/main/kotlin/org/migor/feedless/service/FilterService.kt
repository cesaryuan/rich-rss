package org.migor.feedless.service

import org.apache.commons.lang3.StringUtils
import org.migor.feedless.api.dto.RichArticle
import org.migor.feedless.data.jpa.models.WebDocumentEntity
import org.migor.feedless.harvest.entryfilter.simple.SimpleArticle
import org.migor.feedless.harvest.entryfilter.simple.generated.SimpleArticleFilter
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import java.util.*

@Service
class FilterService {

  private val log = LoggerFactory.getLogger(FilterService::class.simpleName)

  fun matches(corrId: String, article: RichArticle, filter: String?): Boolean {
    return matches(article.url, article.title, article.contentText, article.contentRaw, filter)
  }

  fun matches(
    corrId: String,
    article: WebDocumentEntity,
    filter: String?
  ): Boolean {
    return matches(article.url, article.title!!, StringUtils.trimToEmpty(article.contentText), article.contentRaw, filter)
  }

  private fun matches(
    url: String,
    title: String,
    body: String,
    raw: String?,
    filter: String?
  ): Boolean {
    val matches = Optional.ofNullable(StringUtils.trimToNull(filter))
      .map {
        runCatching {
          val article = SimpleArticle(title, url, body)
          SimpleArticleFilter(it.byteInputStream()).matches(article)
        }.getOrElse { throw RuntimeException("Filter expression is invalid: ${it.message}") }
      }.orElse(true)

    if (matches) {
      log.debug("keep $url")
    } else {
      log.debug("drop $url")
    }
    return matches
  }
}
