package codes.shiftmc.animation.data

import com.github.shynixn.mccoroutine.bukkit.ticks
import kotlinx.coroutines.delay
import org.bukkit.Location
import org.bukkit.Material
import org.bukkit.entity.BlockDisplay
import org.bukkit.plugin.java.JavaPlugin
import org.bukkit.util.Transformation
import org.joml.AxisAngle4f
import org.joml.Vector3f
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin
import kotlin.time.Duration.Companion.seconds

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

    suspend fun move(end: Location, duration: Int) {
        assert(duration > 0) { "Duration must be greater than 0" }

        val differenceVector = end.toVector().subtract(position.toVector())

        // Check if the difference vector is non-zero
        if (differenceVector.lengthSquared() == 0.0) {
            println("Skipping rotation due to zero-length difference vector")
            return
        }

        val directionVector = differenceVector.normalize()

        // Ensure the direction vector is finite
        if (directionVector.x.isFinite() && directionVector.z.isFinite()) {
            val yaw = Math.toDegrees(atan2(directionVector.z, directionVector.x)).toFloat() - 90 // Adjust for Minecraft's coordinate system
            println("Yaw: $yaw")
            rotate(yaw)
        } else {
            println("Direction vector components are not finite: x=${directionVector.x}, z=${directionVector.z}")
        }

        val pathLocations = mutableListOf<Location>()

        // Pre-calculate path
        repeat(duration + 1) {
            val fraction = it / duration.toDouble()
            val intermediateLocation = position.clone().add(
                end.toVector().subtract(position.clone().toVector()).multiply(fraction)
            )
            pathLocations.add(intermediateLocation)
        }

        repeat(duration) {
            move(pathLocations[it])
            delay(1.ticks)
        }
    }

    fun move(location: Location) {
        // Keep the current yaw and pitch
        location.yaw = this.position.yaw
        location.pitch = this.position.pitch

        // Update the position of the vehicle
        this.position = location

        // Move each block to the new location with the same rotation
        blocks.forEach { block ->
            block.blockDisplay?.teleport(location.offseted(block.offset.x, block.offset.y, block.offset.z))
        }

        rotate(yaw)
    }

    var yaw = 0f

    fun rotate(yaw: Float, pitch: Float = 0f) {
        this.yaw = yaw;
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
