package me.socure.extensible.processor.exp1

import me.socure.extensible.processor.model.UnixTimestamp
import me.socure.extensible.processor.{ErrorHandler, PostProcessor}
import scalaz.zio.IO

object LoginAuditor extends PostProcessor[LoginProcessingError, LoginInput, LoginResult] with ErrorHandler[LoginProcessingError, LoginInput, LoginResult] {
  override def postProcess(
                            startTime: UnixTimestamp,
                            actualProcessStartTime: UnixTimestamp,
                            originalInput: LoginInput,
                            processedInput: LoginInput,
                            output: LoginResult): IO[LoginProcessingError, LoginResult] = {
    println(s"SUCCESS :: StartTime=[$startTime], ActualProcessStartTime=[$actualProcessStartTime], OriginalInput=[$originalInput], ProcessedInput=[$processedInput], Output=[$output]")
    IO.now(output)
  }

  override def tryRecover(startTime: UnixTimestamp, actualProcessStartTime: Option[UnixTimestamp], originalInput: LoginInput, processedInput: Option[LoginInput], error: LoginProcessingError): IO[LoginProcessingError, LoginResult] = {
    println(s"FAILURE :: StartTime=[$startTime], ActualProcessorStartTime=[$actualProcessStartTime], OriginalInput=[$originalInput], ProcessedInput=[$processedInput], Error=[$error]")
    IO.fail(error)
  }
}
