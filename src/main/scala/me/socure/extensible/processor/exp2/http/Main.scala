package me.socure.extensible.processor.exp2.http

import dispatch.{Http, url}
import me.socure.extensible.processor.model.{TimeContext, TimingContextProcessor}
import scalaz.zio
import scalaz.zio.{IO, IOApp}

import scala.concurrent.ExecutionContext.Implicits.global

object Main extends IOApp {
  override def run(args: List[String]): IO[zio.Void, Main.ExitStatus] = {
    val processor = new HttpProcessor[TimeContext](Http.default)
      .withContextProcessor(TimingContextProcessor[Throwable])
      .withPostProcessor(HttpAuditor)
      .withErrorHandler(HttpAuditor)
    val req = url("https://reqres.in/api/users?page=2")
    val result = processor.process(req, TimeContext.empty)
    result.attempt.map {
      case Right(res) =>
        println(s"SUCCESS : $res")
        ExitStatus.ExitNow(0)
      case Left(err) =>
        println(s"FAILURE : $err")
        ExitStatus.ExitNow(1)
    }
  }
}
