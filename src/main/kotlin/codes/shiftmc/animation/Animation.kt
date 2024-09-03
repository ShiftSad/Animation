package codes.shiftmc.animation

import codes.shiftmc.animation.data.Offset
import codes.shiftmc.animation.data.SmallBlock
import codes.shiftmc.animation.data.Vehicle
import codes.shiftmc.animation.path.Node
import codes.shiftmc.animation.path.PathCreator
import codes.shiftmc.animation.path.PathManager
import com.sk89q.worldedit.WorldEdit
import com.sk89q.worldedit.bukkit.BukkitAdapter
import dev.jorel.commandapi.CommandAPICommand
import dev.jorel.commandapi.arguments.FloatArgument
import dev.jorel.commandapi.arguments.TextArgument
import dev.jorel.commandapi.executors.PlayerCommandExecutor
import org.bukkit.Bukkit
import org.bukkit.Material
import org.bukkit.block.Block
import org.bukkit.plugin.java.JavaPlugin

class Animation : JavaPlugin() {

    private var vehicle: Vehicle? = null

    override fun onEnable() {
        setStaticPlugin(this)
        PathCreator(this)

        CommandAPICommand("turn")
            .withOptionalArguments(
                FloatArgument("size")
            )
            .executesPlayer(PlayerCommandExecutor { player, args ->
                val size: Float = args.getOrDefault("size", 0.5f) as Float
                vehicle?.kill()

                val bPlayer = BukkitAdapter.adapt(player)

                val region = WorldEdit.getInstance().sessionManager[bPlayer].getSelection(bPlayer.world) ?: run {
                    player.sendMessage("You need to select a region first")
                    return@PlayerCommandExecutor
                }

                val min = region.minimumPoint
                val max = region.maximumPoint

                val blocks = mutableListOf<SmallBlock>()

                // Filter out air blocks
                val filter = setOf(Material.AIR, Material.VOID_AIR, Material.CAVE_AIR, Material.STRUCTURE_VOID)
                val filteredBlocks = mutableListOf<Block>()
                for (x in min.x()..max.x()) for (y in min.y()..max.y()) for (z in min.z()..max.z()) {
                    val block = player.world.getBlockAt(x, y, z)
                    if (block.type !in filter) {
                        filteredBlocks += block
                    }
                }

                // Calculate min and max ignoring air blocks
                val minX = filteredBlocks.minOf { it.x }
                val minY = filteredBlocks.minOf { it.y }
                val minZ = filteredBlocks.minOf { it.z }
                val maxX = filteredBlocks.maxOf { it.x }
                val maxZ = filteredBlocks.maxOf { it.z }

                val centerX = (minX + maxX) / 2.0
                val centerZ = (minZ + maxZ) / 2.0

                val centerPoint = org.bukkit.util.Vector(centerX, minY.toDouble(), centerZ)

                // Loop through filtered blocks to create SmallBlock instances
                for (block in filteredBlocks) {
                    val data = block.blockData
                    val offset = Offset(block.x - centerPoint.x, block.y - centerPoint.y, block.z - centerPoint.z)
                    blocks += SmallBlock(offset, data)
                }

                vehicle = Vehicle(blocks, size, player.location, this)
            })
            .register(this)

        CommandAPICommand("move")
            .withOptionalArguments(TextArgument("node"))
            .executesPlayer(PlayerCommandExecutor { player, args ->
                val nodeArg = args.getOrDefault("node", "here") as String
                if (nodeArg == "here") {
                    vehicle?.move(player.location)
                    return@PlayerCommandExecutor
                }

                val node = PathManager.getNode(nodeArg) ?: run {
                    player.sendMessage("Node not found")
                    return@PlayerCommandExecutor
                }

                vehicle?.move(node.location().apply { y += 1.5 })
                moveVehicle(node, vehicle!!)
            })
            .register(this)

//        scheduleRepeatingTask(0, 1) {
//            vehicle?.rotate(Bukkit.getOnlinePlayers().first().location.yaw)
//        }
    }

    private fun moveVehicle(currentNode: Node, vehicle: Vehicle) {
        val nextNode = currentNode.connect ?: return

        val distance = currentNode.location().distance(nextNode.location())
        val speed = 30.0 // Speed in km/h
        val interval = (distance / speed) * 20 // Convert to milliseconds

        Bukkit.getScheduler().runTaskLater(this, Runnable {
            vehicle.move(nextNode.location().apply { y += 1.5 }, interval.toInt())
            moveVehicle(nextNode, vehicle)
        }, interval.toLong())
    }

}
