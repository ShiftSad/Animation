package codes.shiftmc.animation.data

import org.bukkit.block.data.BlockData
import org.bukkit.entity.BlockDisplay

data class SmallBlock(
    val offset: Offset,
    val blockData: BlockData,
    var blockDisplay: BlockDisplay? = null
)