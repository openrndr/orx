//import org.openrndr.CursorType
//import org.openrndr.application
//
//fun main() {
//    application {
//        program {
//            keyboard.character.listen {
//                if (it.character == 'c') {
//                    mouse.cursorVisible = !mouse.cursorVisible
//                }
//            }
//            extend {
//                if (mouse.position.x < width/2.0) {
//                    mouse.cursorType = CursorType.ARROW_CURSOR
//                } else {
//                    mouse.cursorType = CursorType.HAND_CURSOR
//                }
//            }
//        }
//    }
//}