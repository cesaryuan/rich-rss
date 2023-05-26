package org.migor.feedless.trigger.plugins.graph

import org.migor.feedless.AppProfiles
import org.migor.feedless.data.jpa.models.ArticleEntity
import org.migor.feedless.data.jpa.models.HyperLinkEntity
import org.migor.feedless.data.jpa.models.WebDocumentEntity
import org.migor.feedless.data.jpa.repositories.HyperLinkDAO
import org.migor.feedless.data.jpa.repositories.WebDocumentDAO
import org.migor.feedless.service.FeedService
import org.migor.feedless.service.PluginsService
import org.migor.feedless.util.HtmlUtil
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.context.annotation.Lazy
import org.springframework.context.annotation.Profile
import org.springframework.core.env.Environment
import org.springframework.core.env.Profiles
import org.springframework.data.domain.PageRequest
import org.springframework.stereotype.Service
import java.net.URL
import java.util.*

data class LinkTarget(val url: URL, val text: String)

@Service
@Profile(AppProfiles.webGraph)
class WebGraphPluginImpl: WebGraphPlugin() {

  private val log = LoggerFactory.getLogger(WebGraphPlugin::class.simpleName)

  @Autowired
  lateinit var environment: Environment

  @Autowired
  lateinit var hyperLinkDAO: HyperLinkDAO

  @Autowired
  lateinit var webDocumentDAO: WebDocumentDAO

  @Autowired
  @Lazy
  lateinit var pluginsService: PluginsService

  override fun findOutgoingLinks(article: ArticleEntity, pageable: PageRequest): List<WebDocumentEntity> {
//    return webDocumentDAO.findAllOutgoingHyperLinksByContentId(article.contentId, pageable)
    return emptyList()
  }

  override fun processWebDocument(corrId: String, webDocument: WebDocumentEntity) {
    log.info("[${corrId}] recordOutgoingLink ${webDocument.id}")

    hyperLinkDAO.deleteAllByFromId(webDocument.id)

    val webDocuments = mutableSetOf<WebDocumentEntity>()
    val linkTargets = extractLinkTargets(corrId, webDocument)
    val linkScore = 1.0 / linkTargets.size

    val hyperlinks = linkTargets.map {
      run {
        val url = it.url.toString()
        val destination = Optional.ofNullable(webDocuments.find { it.url == url })
          .orElseGet {
            webDocumentDAO.findByUrlOrAliasUrl(url, url) ?: run {
              val to = WebDocumentEntity()
              to.url = url
              to.pendingPlugins = pluginsService.availablePlugins().map { it.id() }
              to.finalized = false
              to.releasedAt = Date()
              to.updatedAt = Date()
              webDocuments.add(to)
              to
            }
          }

        val link = HyperLinkEntity()
        link.from = webDocument
        link.to = destination
        link.hyperText = it.text
        link.relevance = linkScore
        link
      }
    }
    webDocumentDAO.saveAll(webDocuments)
    hyperLinkDAO.saveAll(hyperlinks)
  }

  override fun configurableInUserProfileOnly(): Boolean  = false
  override fun enabled(): Boolean {
    return environment.acceptsProfiles(Profiles.of(AppProfiles.webGraph))
  }

  private fun extractLinkTargets(corrId: String, webDocument: WebDocumentEntity): List<LinkTarget> {
    webDocument.contentHtml()?.let {
      val doc = HtmlUtil.parseHtml(it, webDocument.url)
      val fromUrl = webDocument.url
      return doc.body().select("a[href]").mapNotNull { link ->
        try {
          LinkTarget(URL(FeedService.absUrl(fromUrl, link.attr("href"))), link.text())
        } catch (e: Exception) {
          log.warn("[${corrId}] ${e.message}")
          null
        }
      }
        .distinct()
        .filter { isNotBlacklisted(it) }
    }
    return emptyList()
  }

  private fun isNotBlacklisted(linkTarget: LinkTarget): Boolean {
    return arrayOf(
      "facebook.com",
      "twitter.com",
      "amazon.com",
      "patreon.com"
    ).none { blackListedUrl -> linkTarget.url.host.contains(blackListedUrl) }
  }

}
