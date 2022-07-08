package org.migor.rich.rss.pipeline

import org.migor.rich.rss.database.model.ArticleHookSpec
import org.migor.rich.rss.database.model.Bucket
import org.migor.rich.rss.database.model.NamespacedTag
import org.migor.rich.rss.database.repository.ArticleRepository
import org.migor.rich.rss.harvest.ArticleSnapshot
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.context.annotation.Profile
import org.springframework.stereotype.Service
import javax.annotation.PostConstruct

@Service
@Profile("stateful")
class PipelineService internal constructor() {

  private lateinit var hookImplementations: List<PipelineHook>
  private val log = LoggerFactory.getLogger(PipelineService::class.simpleName)

  @Autowired
  lateinit var articleRepository: ArticleRepository

  @Autowired
  lateinit var shellCommandHook: ShellCommandHook

  @Autowired
  lateinit var ytArchiverHook: YtArchiverHook

  @Autowired
  lateinit var filterHook: FilterHook

  @Autowired
  lateinit var taggerHook: TaggerHook

  @PostConstruct
  fun onInit() {
    hookImplementations = listOf(shellCommandHook, ytArchiverHook, filterHook, taggerHook)
  }

  fun triggerPipeline(
    corrId: String,
    hookSpecs: List<ArticleHookSpec>,
    article: ArticleSnapshot,
    bucket: Bucket
  ): Triple<ArticleSnapshot, List<NamespacedTag>, Map<String, String>>? {
    val additionalData = mutableMapOf<String, String>()
    val tags = mutableListOf<NamespacedTag>()
    val applied = hookSpecs.takeWhile { hookSpec ->
      hookImplementations.first { hookImplementation -> hookImplementation.type() == hookSpec.type }
        .process(
          corrId,
          article,
          bucket,
          hookSpec,
          { tag: NamespacedTag -> tags.add(tag) }
        ) { data: Pair<String, String> -> additionalData.putIfAbsent(data.first, data.second) }
    }
    return if (applied.size == hookSpecs.size) {
      Triple(article, tags, additionalData)
    } else {
      null
    }
  }
}