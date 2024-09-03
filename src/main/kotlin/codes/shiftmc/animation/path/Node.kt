package codes.shiftmc.animation.path

import com.google.gson.*
import org.bukkit.Bukkit
import org.bukkit.Location
import com.google.gson.annotations.Expose
import java.lang.reflect.Type

data class Node(
    val name: String,
    val world: String,
    val x: Double,
    val y: Double,
    val z: Double,
    var connect: Node? = null
) {
    @Transient
    var connectName: String? = null // Temporary field to store the connect name

    fun location() = Location(Bukkit.getWorld(world), x, y, z)
}

class NodeSerializer : JsonSerializer<Node> {
    override fun serialize(src: Node, typeOfSrc: Type, context: JsonSerializationContext): JsonElement {
        val jsonObject = JsonObject()
        jsonObject.addProperty("name", src.name)
        jsonObject.addProperty("world", src.world)
        jsonObject.addProperty("x", src.x)
        jsonObject.addProperty("y", src.y)
        jsonObject.addProperty("z", src.z)
        jsonObject.addProperty("connect", src.connect?.name) // Save only the reference
        return jsonObject
    }
}

class NodeDeserializer : JsonDeserializer<Node> {
    override fun deserialize(json: JsonElement, typeOfT: Type, context: JsonDeserializationContext): Node {
        val jsonObject = json.asJsonObject
        val name = jsonObject.get("name").asString
        val world = jsonObject.get("world").asString
        val x = jsonObject.get("x").asDouble
        val y = jsonObject.get("y").asDouble
        val z = jsonObject.get("z").asDouble
        val connectName = jsonObject.get("connect")?.asString

        return Node(name, world, x, y, z).apply {
            this.connectName = connectName // Temporarily store the connect name
        }
    }
}