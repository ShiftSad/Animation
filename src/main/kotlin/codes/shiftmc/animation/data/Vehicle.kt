package codes.shiftmc.animation.data

import org.bukkit.Color
import org.bukkit.Location
import org.bukkit.Material
import org.bukkit.Particle
import org.bukkit.entity.BlockDisplay
import org.bukkit.plugin.java.JavaPlugin
import org.bukkit.util.Transformation
import org.joml.AxisAngle4f
import org.joml.Vector3f
import kotlin.math.cos
import kotlin.math.sin

data class Vehicle(
    val blocks: List<SmallBlock>,
    val size: Float,
    private var position: Location,
    val plugin: JavaPlugin
) {
    private val blockedMaterials = setOf(Material.AIR, Material.VOID_AIR, Material.CAVE_AIR)

    private var min: Vector3f
    private var max: Vector3f

    init {
        require(size > 0) { "Size must be greater than 0" }

        // Get min and max ignoring air blocks and void
        blocks.filter { it.blockData.material !in blockedMaterials }.let { blocks ->
            min = Vector3f(
                blocks.minOf { it.offset.x.toFloat() },
                blocks.minOf { it.offset.y.toFloat() },
                blocks.minOf { it.offset.z.toFloat() }
            )
            max = Vector3f(
                blocks.maxOf { it.offset.x.toFloat() },
                blocks.maxOf { it.offset.y.toFloat() },
                blocks.maxOf { it.offset.z.toFloat() }
            )

            blocks.forEach { block ->
                spawnDisplay(this.position, block)
            }
        }
    }

    private var movementTask = -1

    fun move(position: Location, duration: Int = 0) {
        // TODO -> Move
    }

    fun rotate(yaw: Float, pitch: Float = 0f) {
        val radiansYaw = Math.toRadians(yaw.toDouble())
        val cosYaw = cos(radiansYaw)
        val sinYaw = sin(radiansYaw)

        blocks.forEach { block ->
            val offset = block.offset
            val direction = Vector3f(offset.x.toFloat(), offset.y.toFloat(), offset.z.toFloat())

            // Rotate around the Y-axis (yaw)
            val rotatedX = direction.x * cosYaw - direction.z * sinYaw
            val rotatedZ = direction.x * sinYaw + direction.z * cosYaw

            val newLocation = position.clone().add(rotatedX * size, direction.y.toDouble() * size, rotatedZ * size)
            block.blockDisplay?.teleport(newLocation.apply {
                this.yaw = yaw
                this.pitch = pitch
            })
        }
    }

    fun kill() {
        blocks.forEach { block ->
            block.blockDisplay?.remove()
        }
    }

    private fun Location.offseted(x: Double, y: Double, z: Double) = clone().add(x * size, y * size, z * size).apply {
        yaw = 0f
        pitch = 0f
    }

    private fun spawnDisplay(location: Location, block: SmallBlock) {
        block.blockDisplay = location.world.spawn(location.offseted(block.offset.x, block.offset.y, block.offset.z), BlockDisplay::class.java) {
            it.block = block.blockData
            it.transformation = Transformation(Vector3f(), AxisAngle4f(), Vector3f( // Scale
                size, size, size
            ), AxisAngle4f())
        }
    }
}
