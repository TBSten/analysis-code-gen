// PATTERN: CRM-T-0281
// TITLE: CopyMapping / useCase / GetItemApiResponse Item to domain Item mapping
// PROCESSORS: cream
// EXPECT: ok
// TAGS: cream.family=useCase
// ORIGIN: test:TBSten/cream@2162ec8d5261/cream-ksp/src/test/resources/snapshots/CopyMappingSnapshotTest/UseCase/GetItemApiResponse Item to domain Item mapping.md
// LICENSE: Apache-2.0
// STATUS: candidate
// FILE: ItemMapping.kt
package me.tbsten.cream.generated

import java.time.Instant
import kotlin.Int
import kotlin.String
import kotlin.jvm.JvmInline
import me.tbsten.cream.CopyMapping

@CopyMapping(
  GetItemApiResponse.Item::class,
  Item::class,
  properties = [CopyMapping.Map(source = "imageUrl", target = "thumbnailUrl")],
)
private object ItemMapping

public data class GetItemApiResponse(
  public val item: Item,
) {
  public data class Item(
    public val itemId: String,
    public val name: String,
    public val price: Int?,
    public val description: String,
    public val imageUrl: String?,
    public val stock: Int,
    public val updatedAt: String,
  )
}

public data class Item(
  public val itemId: ItemId,
  public val name: String,
  public val price: Int?,
  public val description: String,
  public val thumbnailUrl: String?,
  public val stock: Int,
  public val updatedAt: Instant,
)

@JvmInline
public value class ItemId(
  public val `value`: String,
)
