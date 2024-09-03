package codes.shiftmc.animation.data

data class Offset(
    val x: Double,
    val y: Double,
    val z: Double
) {
    constructor(x: Int, y: Int, z: Int) : this(x.toDouble(), y.toDouble(), z.toDouble())
}
