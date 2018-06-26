package me.socure.extensible.processor.exp1

import me.socure.extensible.processor.Processor
import me.socure.extensible.processor.model.{TimeContext, TimingContextProcessor}
import scalaz.zio
import scalaz.zio.{IO, IOApp}

object Main extends IOApp {
  override def run(args: List[String]): IO[zio.Void, Main.ExitStatus] = {
    val expectedLogin = LoginInput("james@socure.me", "james")
        val loginInput = LoginInput("james@socure.me", "james")
//    val loginInput = LoginInput("james@socure.me1", "james")

    val processor: Processor[LoginProcessingError, TimeContext, LoginInput, LoginResult] = new LoginProcessor(expectedLogin)
      .withPreProcessor(InputValidator)
      .withPostProcessor(LoginAuditor)
      .withErrorHandler(LoginAuditor)
      .withContextProcessor(TimingContextProcessor[LoginProcessingError])

    val t: IO[String, Int] = null

    val result = processor.process(loginInput, TimeContext.empty)
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
