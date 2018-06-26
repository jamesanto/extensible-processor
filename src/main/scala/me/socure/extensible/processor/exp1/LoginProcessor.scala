package me.socure.extensible.processor.exp1

import me.socure.extensible.processor.Processor
import me.socure.extensible.processor.model.{ContextualError, ContextualOutput, TimeContext}
import scalaz.zio.IO

class LoginProcessor(expectedLoginInput: LoginInput) extends Processor[LoginProcessingError, TimeContext, LoginInput, LoginResult] {
  override def process(input: LoginInput, ctx: TimeContext): IO[ContextualError[LoginProcessingError, TimeContext], ContextualOutput[LoginResult, TimeContext]] = {
    if (input == expectedLoginInput) {
      IO.now(ContextualOutput(output = LoginResult("Success"), ctx = ctx))
    } else {
      IO.fail(ContextualError(error = InvalidLogin, ctx = ctx))
    }
  }
}
