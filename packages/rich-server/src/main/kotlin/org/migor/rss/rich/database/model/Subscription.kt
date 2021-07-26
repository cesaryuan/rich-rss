package org.migor.rss.rich.database.model

import org.hibernate.annotations.GenericGenerator
import org.migor.rss.rich.service.Readability
import org.migor.rss.rich.util.JsonUtil
import java.util.*
import javax.persistence.*
import javax.validation.constraints.NotNull


@Entity
@Table(name = "Subscription")
class Subscription {

  @Id
  @GeneratedValue(generator = "uuid")
  @GenericGenerator(name = "uuid", strategy = "uuid2")
  var id: String? = null

  @NotNull
  @Column(name = "tags", columnDefinition = "JSON")
  var tagsJson: String? = null

  @Transient
  var tags: Array<String>? = null

  @NotNull
  @Column(name = "ownerId")
  var ownerId: String? = null

  @NotNull
  @Column(name = "bucketId")
  var bucketId: String? = null

  @NotNull
  @Column(name = "throttleId")
  var throttleId: String? = null

  @NotNull
  @Column(name = "feedId")
  var feedId: String? = null

  @Temporal(TemporalType.TIMESTAMP)
  @Column(name = "updatedAt")
  var updatedAt: Date? = null

  @PrePersist
  @PreUpdate
  fun prePersist() {
    tagsJson = JsonUtil.gson.toJson(tags)
  }

  @PostLoad
  fun postLoad() {
    tags = JsonUtil.gson.fromJson(tagsJson, Array<String>::class.java)
  }
}
