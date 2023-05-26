package org.migor.feedless.data.jpa.models

import com.vladmihalcea.hibernate.type.json.JsonType
import jakarta.persistence.Basic
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.FetchType
import jakarta.persistence.OneToOne
import jakarta.persistence.Table
import org.hibernate.annotations.Type
import org.migor.feedless.data.jpa.EntityWithUUID
import org.migor.feedless.web.GenericFeedSpecification

@Entity
@Table(name = "t_feed_generic")
open class GenericFeedEntity : EntityWithUUID() {

  @Type(JsonType::class)
  @Column(columnDefinition = "jsonb", nullable = false)
  @Basic(fetch = FetchType.LAZY)
  open lateinit var feedSpecification: GenericFeedSpecification

  @Basic
  @Column(nullable = false)
  open lateinit var websiteUrl: String

  @OneToOne(fetch = FetchType.LAZY)
//  @JoinColumn(name = "native_feed_id", referencedColumnName = "id")
  open var nativeFeed: NativeFeedEntity? = null
//
//  @Basic
//  @Column(name = "native_feed_id", nullable = false, insertable = false, updatable = false)
//  open var nativeFeedId: UUID? = null

//  @ManyToMany(fetch = FetchType.LAZY, cascade = [CascadeType.PERSIST])
//  @JoinTable(
//    name = "map_generic_feed_to_tag",
//    joinColumns = [
//      JoinColumn(
//        name = "generic_feed_id", referencedColumnName = "id",
//        nullable = false, updatable = false
//      )],
//    inverseJoinColumns = [
//      JoinColumn(
//        name = "tag_id", referencedColumnName = "id",
//        nullable = false, updatable = false
//      )
//    ]
//  )
//  open var tags: List<TagEntity> = mutableListOf()

}
