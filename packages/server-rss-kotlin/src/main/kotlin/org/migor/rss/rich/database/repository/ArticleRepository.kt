package org.migor.rss.rich.database.repository

import org.migor.rss.rich.database.model.Article
import org.springframework.data.domain.PageRequest
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.PagingAndSortingRepository
import org.springframework.data.repository.query.Param
import org.springframework.stereotype.Repository
import java.util.*
import java.util.stream.Stream

@Repository
interface ArticleRepository : PagingAndSortingRepository<Article, String> {
  fun findByUrl(url: String): Article?

  @Query(
    """select a, r.createdAt from Article a
    inner join ArticleRef r on r.articleId = a.id
    inner join ArticleRefToStream a2s on a2s.id.streamId = ?1 and a2s.id.articleRefId = r.id
    order by r.createdAt DESC """
  )
  fun findAllByStreamId(streamId: String, pageable: PageRequest): List<Array<Any>>

  @Query(
    """select a, f, sub from Article a
    inner join ArticleRef r on r.articleId = a.id
    inner join ArticleRefToStream l on l.id.articleRefId = r.id
    inner join Feed f on f.streamId = l.id.streamId
    inner join Subscription sub on sub.feedId = f.id
    where (
        (sub.lastUpdatedAt is null and f.lastUpdatedAt is not null)
        or
        (sub.lastUpdatedAt < f.lastUpdatedAt)
    ) and sub.id = :subscriptionId
    order by a.score desc, r.createdAt asc """
  )
  fun findNewArticlesForSubscription(@Param("subscriptionId") subscriptionId: String): Stream<Array<Any>>

//  @Query(
//    """select distinct a from Article a
//    inner join ArticleRef r on r.articleId = a.id
//    inner join ArticleRefToStream l on l.id.articleRefId = r.id
//    inner join Bucket b on l.id.streamId = b.streamId
//    where b.id = :bucketId and r.createdAt > :lastPostProcessedAt and a.applyPostProcessors = true"""
//  )
//  fun findAllNewArticlesInBucketId(@Param("bucketId") bucketId: String, @Param("lastPostProcessedAt") lastPostProcessedAt: Date?): List<Article>

  @Query(
    """select a from Article a
        inner join ArticleRef r on r.articleId = a.id
        inner join ArticleRefToStream l on l.id.articleRefId = r.id
        where a.url = :url and l.id.streamId = :streamId
    """
  )
  fun findInStream(@Param("url") url: String, @Param("streamId") streamId: String): Article?

  @Query(
    """select a, f, s from Article a
    inner join ArticleRef r on r.articleId = a.id
    inner join ArticleRefToStream l on l.id.articleRefId = r.id
    inner join Stream s on s.id = l.id.streamId
    inner join Feed f on f.streamId = s.id
    where f.id in ?1 and r.createdAt >= ?2"""
  )
  fun findAllThrottled(
    feedIds: List<String>,
    articlesAfter: Date,
    pageable: PageRequest
  ): Stream<Array<Any>>
}
