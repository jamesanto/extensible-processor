package me.socure.extensible.processor.exp1

import me.socure.extensible.processor.PreProcessor
import scalaz.zio.IO

object InputValidator extends PreProcessor[LoginProcessingError, LoginInput] {
  override def preProcess(input: LoginInput): IO[LoginProcessingError, LoginInput] = {
    if (input.username.contains("@")) {
      IO.now(input)
    } else {
      IO.fail(InvalidEmail)
    }
  }
}
