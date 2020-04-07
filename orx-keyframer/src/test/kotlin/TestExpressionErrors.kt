import org.amshove.kluent.`should throw`
import org.amshove.kluent.`with message`
import org.amshove.kluent.invoking
import org.openrndr.extra.keyframer.ExpressionException
import org.openrndr.extra.keyframer.evaluateExpression
import org.spekframework.spek2.Spek
import org.spekframework.spek2.style.specification.describe
import java.lang.IllegalStateException

object TestExpressionErrors : Spek({

    describe("an expression with non-sensible writing") {
        val expression = ")("
        it("should cause an exception to be thrown when evaluated") {
            invoking {
                evaluateExpression(expression)
            } `should throw`  ExpressionException::class `with message` "parser error in expression: ')('; [line: 1, character: 0 , near: [@0,0:0=')',<21>,1:0] ]"
        }
    }

    describe("an expression with equality instead of assign") {
        val expression = "a == 5"
        it("should cause an exception to be thrown when evaluated") {
            invoking {
                evaluateExpression(expression)
            }  `should throw` ExpressionException::class `with message` "parser error in expression: 'a == 5'; [line: 1, character: 3 , near: [@3,3:3='=',<19>,1:3] ]"
        }
    }

    describe("an expression trying to reassign a number") {
        val expression = "3 = 5"
        it("should cause an exception to be thrown when evaluated") {
            invoking {
               evaluateExpression(expression)
            } `should throw` ExpressionException::class `with message` "parser error in expression: '3 = 5'; [line: 1, character: 2 , near: [@2,2:2='=',<19>,1:2] ]"
        }
    }

    describe("an expression that uses non-existing functions") {
        val expression = "notExisting(5)"
        it("should cause an exception to be thrown when evaluated") {
            invoking {
                evaluateExpression(expression)
            } `should throw` ExpressionException::class `with message` "error in evaluation of 'notExisting(5)': unresolved function: 'notExisting(x0)'"
        }
    }

    describe("an expression that uses non-existing variables") {
        val expression = "notExisting + 4"
        it("should cause an exception to be thrown when evaluated") {
            invoking {
                evaluateExpression(expression)
            } `should throw` ExpressionException::class `with message` "error in evaluation of 'notExisting+4': unresolved variable: 'notExisting'"
        }
    }

})
