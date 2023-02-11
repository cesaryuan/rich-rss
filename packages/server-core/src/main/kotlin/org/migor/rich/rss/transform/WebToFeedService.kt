package org.migor.rich.rss.transform

import org.apache.commons.lang3.StringUtils
import org.jsoup.nodes.Document
import org.jsoup.nodes.Element
import org.jsoup.nodes.TextNode
import org.migor.rich.rss.api.dto.RichArticle
import org.migor.rich.rss.api.dto.RichFeed
import org.migor.rich.rss.harvest.ArticleRecovery
import org.migor.rich.rss.service.AuthToken
import org.migor.rich.rss.service.FeedService.Companion.absUrl
import org.migor.rich.rss.service.FilterService
import org.migor.rich.rss.service.HttpService
import org.migor.rich.rss.service.PropertyService
import org.migor.rich.rss.service.PuppeteerService
import org.migor.rich.rss.util.FeedUtil
import org.migor.rich.rss.util.HtmlUtil
import org.migor.rich.rss.util.HtmlUtil.parseHtml
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service
import us.codecraft.xsoup.Xsoup
import java.net.URL
import java.net.URLEncoder
import java.nio.charset.StandardCharsets
import java.util.*

@Service
class WebToFeedService {

  private val log = LoggerFactory.getLogger(WebToFeedService::class.simpleName)

  @Autowired
  lateinit var httpService: HttpService

  @Autowired
  lateinit var propertyService: PropertyService

  @Autowired
  lateinit var webToFeedTransformer: WebToFeedTransformer

  @Autowired
  lateinit var articleRecovery: ArticleRecovery

  @Autowired
  lateinit var filterService: FilterService

  @Autowired
  lateinit var puppeteerService: PuppeteerService

  @Value("\${app.publicUrl}")
  lateinit var appPublicUrl: String

  fun applyRule(
    corrId: String,
    feedUrl: String,
    selectors: GenericFeedSelectors,
    fetchOptions: GenericFeedFetchOptions,
    parserOptions: GenericFeedParserOptions,
    refineOptions: GenericFeedRefineOptions,
    token: AuthToken,
  ): RichFeed {
    val url = fetchOptions.websiteUrl
    log.debug("[${corrId}] applyRule")

    validateVersion(parserOptions.version)
    httpService.guardedHttpResource(corrId, url, 200, listOf("text/", "application/xml", "application/json", "application/rss", "application/atom", "application/rdf"))

    val markup = if (fetchOptions.prerender) {
      val puppeteerResponse =
        puppeteerService.prerender(corrId, url, fetchOptions)
      puppeteerResponse.html!!
    } else {
      val response = httpService.httpGetCaching(corrId, url, 200)
      String(response.responseBody, Charsets.UTF_8)
    }

    val doc = parseHtml(markup, url)
    val recovery = refineOptions.recovery
    val items = webToFeedTransformer.getArticlesBySelectors(corrId, selectors, doc, URL(url))
      .asSequence()
      .filterIndexed { index, _ -> articleRecovery.shouldRecover(recovery, index) }
      .map { articleRecovery.recoverAndMerge(corrId, it, recovery) }
      .filter { filterService.matches(corrId, it, refineOptions.filter) }
      .toList()

    val nextUrl = Optional.ofNullable(getNextUrlUsingPagination(doc, selectors.paginationXPath, url))
      .map { webToFeedTransformer.createFeedUrl(URL(it), selectors, parserOptions, fetchOptions, refineOptions) }
      .orElse(null)
    return createFeed(url, doc.title(), items, feedUrl, nextUrl)
  }

  private fun getNextUrlUsingPagination(doc: Document, paginationXPath: String?, url: String): String? {
    return if (StringUtils.isBlank(paginationXPath)) {
      null
    } else {
      Optional.ofNullable(Xsoup.compile(paginationXPath).evaluate(doc).elements.firstOrNull())
        .map { paginationContext -> run {
          // cleanup
          paginationContext.childNodes().filterIsInstance<TextNode>()
            .filter { it.text().trim().replace(Regex("[^a-zA-Z<>0-9]+"), "").isEmpty() }
            .forEach { it.remove() }

          paginationContext.childNodes().filterIsInstance<TextNode>().forEach { it.replaceWith(toSpan(it)) }

          // detect anomaly
          val links = paginationContext.select("a[href]").map {
            run {
              var element = it
              while(element.parent() != paginationContext) {
                element = element.parent()
              }
              element
            }
          }

          val children = paginationContext.children()
          val relativeNextUrl =
            children.dropWhile { links.contains(it) }
              .first { links.contains(it) }
              .select("a[href]").attr("href")

          absUrl(url, relativeNextUrl)
        }
        }.orElse(null)
    }
  }

  private fun toSpan(it: TextNode): Element {
    val span = Element("span")
    span.text(it.text())
    return span
  }

  private fun createFeed(
    homePageUrl: String,
    title: String,
    items: List<RichArticle>,
    feedUrl: String,
    nextPage: String? = null
  ): RichFeed {
    val richFeed = RichFeed()
    richFeed.id = feedUrl
    richFeed.title = title
    richFeed.websiteUrl = homePageUrl
    richFeed.publishedAt = Date()
    richFeed.items = items
    richFeed.feedUrl = feedUrl
    richFeed.nextUrl = nextPage
    richFeed.lastPage = null
    richFeed.editUrl = "/wizard?feedUrl=${URLEncoder.encode(feedUrl, StandardCharsets.UTF_8)}"
    return richFeed
  }
  fun createMaintenanceFeed(corrId: String, homePageUrl: String, feedUrl: String, article: RichArticle): RichFeed {
    log.info("[${corrId}] falling back to maintenance feed")
    return createFeed(
      homePageUrl,
      "Maintenance",
      listOf(article),
      feedUrl
    )
  }

  private fun encode(param: String): String = URLEncoder.encode(
    param,
    StandardCharsets.UTF_8
  )

  fun createMaintenanceArticle(e: Throwable, url: String): RichArticle {
    // distinguish if an exception will be permanent or not, and only then send it
    val richArticle = RichArticle()
    richArticle.id = FeedUtil.toURI("maintenance-request", url, Date())
    richArticle.title = "Maintenance required"
    richArticle.contentText = Optional.ofNullable(e.message).orElse(e.toString())
    richArticle.url = "${appPublicUrl}/?reason=${e.message}&url=${encode(url)}"
    richArticle.publishedAt = Date()
    return richArticle
  }

  private fun validateVersion(version: String) {
    if (version != propertyService.webToFeedVersion) {
      throw RuntimeException("Invalid webToFeed Version. Got ${version}, expected ${propertyService.webToFeedVersion}")
    }
  }
}
