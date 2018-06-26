package me.socure.extensible.processor.exp2.http

import dispatch.{Http, Req}
import me.socure.extensible.processor.Processor
import me.socure.extensible.processor.model.{ContextualError, ContextualOutput}
import org.asynchttpclient.Response
import scalaz.zio.{ExitResult, IO}

import scala.concurrent.ExecutionContext
import scala.util.{Failure, Success}

class HttpProcessor[CtxT](http: Http)(implicit ec: ExecutionContext) extends Processor[Throwable, CtxT, Req, Response] {
  override def process(input: Req, ctx: CtxT): IO[ContextualError[Throwable, CtxT], ContextualOutput[Response, CtxT]] = {
    val resFuture = http(req = input)
    IO.async(cb => {
      resFuture.onComplete {
        case Success(response) => cb(ExitResult.Completed(ContextualOutput(output = response, ctx = ctx)))
        case Failure(exception) => cb(ExitResult.Failed(ContextualError(error = exception, ctx = ctx)))
      }
    })
  }
}
