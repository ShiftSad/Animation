package codes.shiftmc.animation.path

import com.google.gson.Gson
import com.google.gson.GsonBuilder
import com.google.gson.JsonArray
import java.io.File

object PathManager {
    private val points = mutableListOf<Node>()
    private val pointFile = File("nodes.json")

    private val gson: Gson = GsonBuilder()
        .setPrettyPrinting()
        .registerTypeAdapter(Node::class.java, NodeDeserializer())
        .registerTypeAdapter(Node::class.java, NodeSerializer())
        .create()

    init {
        if (pointFile.exists()) {
            val nodesJson = pointFile.readText()
            val jsonArray = gson.fromJson(nodesJson, JsonArray::class.java)
            points.addAll(deserializeNodes(jsonArray))
        }
    }

    fun getNode(x: Double, y: Double, z: Double) =
        points.find { it.x == x && it.y == y && it.z == z }

    fun getNode(x: Int, y: Int, z: Int) =
        getNode(x.toDouble(), y.toDouble(), z.toDouble())

    fun getNode(name: String) =
        points.find { it.name == name }

    fun nodes() =
        points.toList()

    fun addNode(node: Node) =
        points.add(node)

    fun removeNode(name: String) {
        val node = points.find { it.name == name }
        if (node != null) {
            points.remove(node)
            points.forEach { it.connect = it.connect?.takeIf { it.name != name } }
        }
    }

    fun connect(node1: String, node2: String) {
        val n1 = points.find { it.name == node1 }
        val n2 = points.find { it.name == node2 }
        if (n1 != null && n2 != null) {
            n1.connect = n2
        }
    }

    fun save() {
        val jsonArray = points.map { gson.toJsonTree(it) }
        pointFile.writeText(gson.toJson(JsonArray().apply { jsonArray.forEach { add(it) } }))
    }

    private fun deserializeNodes(json: JsonArray): List<Node> {
        val gson = GsonBuilder()
            .registerTypeAdapter(Node::class.java, NodeDeserializer())
            .create()

        // First pass: Deserialize all nodes without setting connections
        val nodes = json.map { gson.fromJson(it, Node::class.java) }
        val nodeMap = nodes.associateBy { it.name }

        // Second pass: Set the connect field for each node
        nodes.forEach { node ->
            node.connect = node.connectName?.let { nodeMap[it] }
        }

        return nodes
    }
}