package codes.shiftmc.animation.path

import codes.shiftmc.animation.Animation
import codes.shiftmc.animation.events
import codes.shiftmc.animation.scheduleRepeatingTask
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.event.ClickEvent
import net.kyori.adventure.text.format.NamedTextColor
import org.bukkit.*
import org.bukkit.Particle.DustOptions
import org.bukkit.event.player.PlayerDropItemEvent
import org.bukkit.event.player.PlayerInteractEvent
import org.bukkit.event.player.PlayerSwapHandItemsEvent
import org.bukkit.persistence.PersistentDataType
import java.util.*

class PathCreator(
    instance: Animation
) {
    init {
        instance.events {
            // Create a new node or link to an existing node
            event<PlayerInteractEvent> {
                val itemName = item?.itemMeta?.displayName ?: return@event
                if (itemName != "Node Creator") return@event
                if (!player.isOp) return@event
                isCancelled = true


                // The block the player is looking at
                val block = player.getTargetBlockExact(100, FluidCollisionMode.NEVER) ?: return@event
                val node = PathManager.getNode(block.x, block.y, block.z) ?: run {
                    val node = Node(
                        UUID.randomUUID().toString(),
                        block.world.name,
                        block.x.toDouble(),
                        block.y.toDouble(),
                        block.z.toDouble()
                    )

                    PathManager.addNode(node)
                    return@event
                }

                // Retrieve the selected node from the player's PDC
                val container = player.persistentDataContainer
                val selectedNodeKey = NamespacedKey(instance, "selectedNode")

                if (action.isRightClick) {
                    // Save the new node inside the player's PDC
                    container.set(selectedNodeKey, PersistentDataType.STRING, node.name)
                } else if (action.isLeftClick) {
                    val selectedNodeName = container.get(selectedNodeKey, PersistentDataType.STRING)
                    val selectedNode = if (selectedNodeName != null) PathManager.nodes().find { it.name == selectedNodeName } else null

                    if (selectedNode != null) {
                        // Link the selected node to the new node
                        PathManager.connect(selectedNode.name, node.name)
                    }
                }
            }

            // Clear the selected node from the player's PDC
            event<PlayerSwapHandItemsEvent> {
                if (player.inventory.itemInMainHand.itemMeta?.displayName != "Node Creator") return@event
                if (!player.isOp) return@event
                isCancelled = true

                // Clear the selected node from the player's PDC
                val container = player.persistentDataContainer
                val selectedNodeKey = NamespacedKey(instance, "selectedNode")
                PathManager.removeNode(container.get(selectedNodeKey, PersistentDataType.STRING) ?: return@event)
                container.remove(selectedNodeKey)

                PathManager.save()
            }

            // Send info about the selected node to the player
            event<PlayerDropItemEvent> {
                if (itemDrop.itemStack.itemMeta?.displayName != "Node Creator") return@event
                if (!player.isOp) return@event
                isCancelled = true

                // Print the selected node to the console
                val container = player.persistentDataContainer
                val selectedNodeKey = NamespacedKey(instance, "selectedNode")
                val selectedNodeName = container.get(selectedNodeKey, PersistentDataType.STRING)
                player.sendMessage(
                    Component.text("Selected Node: ")
                    .append(
                        Component.text(
                        selectedNodeName ?: "None", NamedTextColor.GREEN
                    ).clickEvent(ClickEvent.suggestCommand(selectedNodeName ?: "")))
                )
            }
        }
        scheduleRepeatingTask(0, 180) { PathManager.save() }

        scheduleRepeatingTask(0, 4) {
            Bukkit.getOnlinePlayers().filter { it.isOp && it.inventory.itemInMainHand.itemMeta?.displayName == "Node Creator" }.forEach { player ->
                val container = player.persistentDataContainer
                val selectedNodeKey = NamespacedKey(instance, "selectedNode")
                val selectedNodeName = container.get(selectedNodeKey, PersistentDataType.STRING)
                val selectedNode = if (selectedNodeName != null) PathManager.nodes().find { it.name == selectedNodeName } else null

                val block = player.getTargetBlockExact(100, FluidCollisionMode.NEVER)
                if (block != null) {
                    player.spawnParticle(Particle.CRIT, block.x + 0.5, block.y + 1.5, block.z + 0.5, 0)
                }

                PathManager.nodes().forEach node@ { node ->
                    player.spawnParticle(
                        if (node == selectedNode) Particle.FLAME else Particle.END_ROD,
                        node.x + 0.5, node.y + 1.5, node.z + 0.5,
                        0
                    )

                    calculateLine(node.location(), node.connect?.location() ?: run {
                        player.world.spawnParticle(Particle.CRIT, node.x + 0.5, node.y + 1.5, node.z + 0.5, 0)
                        return@node
                    }, 0.2).forEach { location ->
                        player.world.spawnParticle(Particle.DUST, location.x + 0.5, location.y + 1.5, location.z + 0.5, 0,
                            DustOptions(Color.RED, 0.5f)
                        )
                    }
                }
            }
        }
    }

    private fun calculateLine(start: Location, end: Location, space: Double): List<Location> {
        val points = mutableListOf<Location>()
        val distance = start.distance(end)
        val vector = end.toVector().subtract(start.toVector()).normalize().multiply(space)
        var current = start.clone()
        for (i in 0 until (distance / space).toInt()) {
            current = current.add(vector)
            points.add(current.clone())
        }
        return points
    }
}