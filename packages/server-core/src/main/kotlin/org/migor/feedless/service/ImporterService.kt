package org.migor.feedless.service

import org.migor.feedless.AppProfiles
import org.migor.feedless.api.auth.CurrentUser
import org.migor.feedless.api.graphql.DtoResolver.fromDTO
import org.migor.feedless.data.jpa.enums.ArticleType
import org.migor.feedless.data.jpa.enums.ReleaseStatus
import org.migor.feedless.data.jpa.models.ArticleEntity
import org.migor.feedless.data.jpa.models.BucketEntity
import org.migor.feedless.data.jpa.models.ImporterEntity
import org.migor.feedless.data.jpa.models.NativeFeedEntity
import org.migor.feedless.data.jpa.models.StreamEntity
import org.migor.feedless.data.jpa.models.UserEntity
import org.migor.feedless.data.jpa.models.WebDocumentEntity
import org.migor.feedless.data.jpa.repositories.ArticleDAO
import org.migor.feedless.data.jpa.repositories.ImporterDAO
import org.migor.feedless.data.jpa.repositories.WebDocumentDAO
import org.migor.feedless.generated.types.ImporterAttributesInput
import org.migor.feedless.generated.types.ImporterUpdateInput
import org.migor.feedless.generated.types.ImportersWhereInput
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.context.annotation.Profile
import org.springframework.data.domain.PageRequest
import org.springframework.security.access.AccessDeniedException
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Propagation
import org.springframework.transaction.annotation.Transactional
import java.util.*

@Service
@Profile(AppProfiles.database)
class ImporterService {

  private val log = LoggerFactory.getLogger(ImporterService::class.simpleName)

  @Autowired
  lateinit var articleDAO: ArticleDAO

  @Autowired
  lateinit var webDocumentDAO: WebDocumentDAO

  @Autowired
  lateinit var importerDAO: ImporterDAO

  @Autowired
  lateinit var currentUser: CurrentUser

  fun importArticleToTargets(
    corrId: String,
    contents: List<WebDocumentEntity>,
    stream: StreamEntity,
    feed: NativeFeedEntity,
    articleType: ArticleType,
    status: ReleaseStatus,
    releasedAt: Date? = null,
    importer: ImporterEntity? = null,
  ) {
    contents.forEach { webDocument ->
      forwardToStream(
        corrId,
        webDocument,
        releasedAt ?: webDocument.releasedAt,
        stream,
        importer,
        feed,
        articleType,
        status
      )
    }

//    targets.forEach { target ->
//      when (target) {
////            ExporterTargetType.email -> forwardAsEmail(corrId, articleId, ownerId, pubDate, refType)
////            ExporterTargetType.webhook -> forwardToWebhook(corrId, article, pubDate, target)
//        else -> log.warn("[${corrId}] Unsupported importerTarget $target")
//      }
//    }
  }

//  private fun forwardAsEmail(
//    corrId: String,
//    articleId: UUID,
//    ownerId: UUID,
//    pubDate: Date,
//    refType: Stream2ArticleEntityType
//  ) {
//    TODO("Not yet implemented")
//  }

  private fun forwardToStream(
    corrId: String,
    webDocument: WebDocumentEntity,
    releasedAt: Date,
    stream: StreamEntity,
    importer: ImporterEntity?,
    feed: NativeFeedEntity,
    type: ArticleType,
    status: ReleaseStatus
  ) {

    importer?.plugins?.let { plugins ->
      if (plugins.isNotEmpty()) {
        val expectedPlugins = plugins.takeUnless { (plugin: String) ->
          webDocument.pendingPlugins.contains(plugin) || webDocument.executedPlugins.contains(plugin)
        }

        expectedPlugins?.let {
          val pendingPlugins = mutableListOf<String>()
          pendingPlugins.addAll(webDocument.pendingPlugins)
          pendingPlugins.addAll(expectedPlugins)
          log.info("[$corrId] queue importer plugins [${expectedPlugins.joinToString(", ")}]")
          webDocument.pendingPlugins = pendingPlugins
          webDocumentDAO.save(webDocument)
        }
      }
    }

    log.debug("[$corrId] append article -> stream ${stream.id}")
    val article = ArticleEntity()
    article.webDocumentId = webDocument.id
    article.ownerId = feed.ownerId
    article.releasedAt = releasedAt
    article.streamId = stream.id
    article.type = type
    article.status = status
    article.importerId = importer?.id
//    article.feed = feed
    articleDAO.save(article)
  }

  @Transactional(propagation = Propagation.REQUIRED)
  fun createImporter(
    corrId: String,
    nativeFeed: NativeFeedEntity,
    bucket: BucketEntity,
    data: ImporterAttributesInput,
    user: UserEntity,
  ): ImporterEntity {
    log.info("[$corrId] create importer")

    assertOwnership(bucket.ownerId)

    val importer = ImporterEntity()
    importer.feedId = nativeFeed.id
    importer.bucketId = bucket.id
    importer.autoRelease = data.autoRelease ?: true
    importer.filter = data.filter
    importer.title = data.title
    importer.ownerId = user.id

    val saved = importerDAO.save(importer)
    log.debug("[${corrId}] created ${saved.id}")
    return saved
  }

  private fun assertOwnership(ownerId: UUID?) {
    if (ownerId != currentUser.userId() && !currentUser.isAdmin()) {
      throw AccessDeniedException("insufficient privileges")
    }
  }

  fun delete(corrId: String, id: UUID) {
    log.debug("[${corrId}] delete $id")
    val importer = importerDAO.findById(id).orElseThrow { IllegalArgumentException("importer not found") }
    assertOwnership(importer.ownerId)

    articleDAO.deleteAllByImporterId(id)
    importerDAO.deleteById(id)
  }

  fun findById(id: UUID): Optional<ImporterEntity> {
    return importerDAO.findById(id)
  }

  fun findAllByFeedId(id: UUID): List<ImporterEntity> {
    return importerDAO.findAllByFeedId(id)
  }

  fun findAllByFilter(where: ImportersWhereInput, pageable: PageRequest): List<ImporterEntity> {
    val buckets = where.buckets?.oneOf?.map { UUID.fromString(it) }
    val status = where.status?.oneOf?.map { fromDTO(it) }
    return importerDAO.findAllByFilter(buckets, status, pageable)
  }

  fun update(corrId: String, data: ImporterUpdateInput): ImporterEntity {
    val importer = importerDAO.findById(UUID.fromString(data.where.id)).orElseThrow()
    assertOwnership(importer.ownerId)

//    data.autoRelease?.let {
      importer.autoRelease = data.importer.autoRelease
//    }
//    data.title?.let {
      importer.title = data.importer.title
//    }
//    data.filter?.let {
      importer.filter = data.importer.filter
//    }
//    data.plugins?.let {
      importer.plugins = data.importer.plugins
//    }
    return importerDAO.save(importer)
  }
}
