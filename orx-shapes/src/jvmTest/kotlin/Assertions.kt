import org.amshove.kluent.shouldBeInRange
import org.openrndr.math.Vector2

infix fun Vector2.`should be near`(other: Vector2) {
    x shouldBeInRange (other.x - 0.00001..other.x + 0.00001)
    y shouldBeInRange (other.y - 0.00001..other.y + 0.00001)
}
