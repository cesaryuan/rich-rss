package org.migor.feedless.data.jpa.models

import com.vladmihalcea.hibernate.type.json.JsonType
import jakarta.persistence.Basic
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.FetchType
import jakarta.persistence.JoinColumn
import jakarta.persistence.ManyToOne
import jakarta.persistence.Table
import jakarta.persistence.Temporal
import jakarta.persistence.TemporalType
import jakarta.persistence.UniqueConstraint
import org.hibernate.annotations.Type
import org.migor.feedless.data.jpa.EntityWithUUID
import org.migor.feedless.data.jpa.StandardJpaFields
import org.migor.feedless.data.jpa.enums.ImporterRefreshTrigger
import java.util.*

@Entity
@Table(
  name = "t_importer",
  uniqueConstraints = [
    UniqueConstraint(columnNames = ["bucketId", "feedId"])
  ]
)
open class ImporterEntity : EntityWithUUID() {

  @Basic
  open var filter: String? = null

  @Temporal(TemporalType.TIMESTAMP)
  @Column
  open var triggerScheduledNextAt: Date? = null

  @Temporal(TemporalType.TIMESTAMP)
  @Column
  open var triggerScheduledLastAt: Date? = null

  @Column(nullable = false)
  @Enumerated(EnumType.STRING)
  open var triggerRefreshOn: ImporterRefreshTrigger = ImporterRefreshTrigger.CHANGE

  @Column
  open var lookAheadMin: Int? = null

  @Column
  open var triggerScheduleExpression: String? = null

  @Column
  open var segmentSortField: String? = null

  @Column(nullable = false)
  open var segmentSortAsc: Boolean = true

  @Basic
  open var title: String? = null

  @Column(name = "segment_digest", nullable = false)
  open var digest: Boolean = false

  @Column(name = "segment_size")
  open var segmentSize: Int? = null

  @Column(name = "bucketId", nullable = true)
  open lateinit var bucketId: UUID

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "bucketId", referencedColumnName = "id", insertable = false, updatable = false)
  open var bucket: BucketEntity? = null // todo mag rename to target

  @ManyToOne(fetch = FetchType.LAZY, cascade = [])
  @JoinColumn(name = "feedId", referencedColumnName = "id", insertable = false, updatable = false)
  open var feed: NativeFeedEntity? = null // todo mag rename to source

  @Basic
  @Column(name = "feedId", nullable = false)
  open lateinit var feedId: UUID

  @Basic
  @Column(name = "is_auto_release")
  open var autoRelease: Boolean = true

  @Temporal(TemporalType.TIMESTAMP)
  @Column
  open var lastUpdatedAt: Date? = null

  @Type(JsonType::class)
  @Column(columnDefinition = "jsonb", nullable = false)
  @Basic(fetch = FetchType.LAZY)
  open var plugins: List<String> = emptyList()

  @Basic
  @Column(name = StandardJpaFields.ownerId, nullable = false)
  open lateinit var ownerId: UUID

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = StandardJpaFields.ownerId, referencedColumnName = "id", insertable = false, updatable = false)
  open var owner: UserEntity? = null

//  https://vladmihalcea.com/map-postgresql-enum-array-jpa-entity-property-hibernate/
//  @Type(EnumArrayType::class)
//  @Transient
//  open var targets: Array<ImporterTargetType> = emptyArray()

//  @Basic
//  @Column(name = StandardJpaFields.ownerId, nullable = true, insertable = false, updatable = false)
//  open var ownerId: UUID? = null
//
//  @ManyToOne(fetch = FetchType.LAZY)
//  @JoinColumn(name = StandardJpaFields.ownerId, referencedColumnName = "id")
//  open var owner: UserEntity? = null
}
