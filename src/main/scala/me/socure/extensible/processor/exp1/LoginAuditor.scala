package me.socure.extensible.processor.exp1

import me.socure.extensible.processor.model.{ContextualError, ContextualOutput, TimeContext}
import me.socure.extensible.processor.{ErrorHandler, PostProcessor}
import scalaz.zio.IO

object LoginAuditor extends PostProcessor[LoginProcessingError, TimeContext, LoginInput, LoginResult] with ErrorHandler[LoginProcessingError, TimeContext, LoginInput, LoginResult] {
  override def postProcess(originalInput: LoginInput, processedInput: LoginInput, output: ContextualOutput[LoginResult, TimeContext]): IO[ContextualError[LoginProcessingError, TimeContext], ContextualOutput[LoginResult, TimeContext]] = {
    println(List(
      "SUCCESS ::",
      s"ORIGINAL_INPUT=[$originalInput]",
      s"PROCESSED_INPUT=[$processedInput]",
      s"OUTPUT=[$output]"
    ).mkString(", "))
    IO.now(output)
  }

  override def tryRecover(originalInput: LoginInput, processedInput: Option[LoginInput], originalCtx: TimeContext, processedCtx: TimeContext, error: LoginProcessingError): IO[ContextualError[LoginProcessingError, TimeContext], ContextualOutput[LoginResult, TimeContext]] = {
    println(List(
      "FAILURE ::",
      s"ORIGINAL_INPUT=[$originalInput]",
      s"PROCESSED_INPUT=[$processedInput]",
      s"ERROR=[$error]"
    ).mkString(", "))
    IO.fail(ContextualError(error = error, ctx = processedCtx))
  }
}
