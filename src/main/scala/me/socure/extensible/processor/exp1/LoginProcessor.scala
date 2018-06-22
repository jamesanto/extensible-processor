package me.socure.extensible.processor.exp1

import me.socure.extensible.processor.Processor
import scalaz.zio.IO

class LoginProcessor(expectedLoginInput: LoginInput) extends Processor[LoginProcessingError, LoginInput, LoginResult] {
  override def doProcess(input: LoginInput): IO[LoginProcessingError, LoginResult] = {
    if (input == expectedLoginInput) {
      IO.now(LoginResult("Success"))
    } else {
      IO.fail(InvalidLogin)
    }
  }
}
