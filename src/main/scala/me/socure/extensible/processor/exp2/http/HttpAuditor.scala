package me.socure.extensible.processor.exp2.http

import dispatch.Req
import me.socure.extensible.processor.model.{ContextualError, ContextualOutput, TimeContext}
import me.socure.extensible.processor.{ErrorHandler, PostProcessor}
import org.asynchttpclient.Response
import scalaz.zio.IO

object HttpAuditor extends PostProcessor[Throwable, TimeContext, Req, Response] with ErrorHandler[Throwable, TimeContext, Req, Response] {
  override def postProcess(originalInput: Req, processedInput: Req, output: ContextualOutput[Response, TimeContext]): IO[ContextualError[Throwable, TimeContext], ContextualOutput[Response, TimeContext]] = {
    println(List(
      "SUCCESS ::",
      s"ORIGINAL_INPUT=[$originalInput]",
      s"PROCESSED_INPUT=[$processedInput]",
      s"OUTPUT=[$output]"
    ).mkString(", "))
    IO.now(output)
  }

  override def tryRecover(originalInput: Req, processedInput: Option[Req], originalCtx: TimeContext, processedCtx: TimeContext, error: Throwable): IO[ContextualError[Throwable, TimeContext], ContextualOutput[Response, TimeContext]] = {
    println(List(
      "FAILURE ::",
      s"ORIGINAL_INPUT=[$originalInput]",
      s"PROCESSED_INPUT=[$processedInput]",
      s"ERROR=[$error]"
    ).mkString(", "))
    IO.fail(ContextualError(error = error, ctx = processedCtx))
  }
}
