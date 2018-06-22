package me.socure.extensible.processor

import me.socure.extensible.processor.model.UnixTimestamp
import scalaz.zio.IO

trait ErrorHandler[ErrorT, InputT, OutputT] {
  def tryRecover(
                  startTime: UnixTimestamp,
                  actualProcessStartTime: Option[UnixTimestamp],
                  originalInput: InputT,
                  processedInput: Option[InputT],
                  error: ErrorT): IO[ErrorT, OutputT]
}

object ErrorHandler {

  implicit class RichErrorHandler[ErrorT, InputT, OutputT](val value: ErrorHandler[ErrorT, InputT, OutputT]) extends AnyVal {
    def and(other: ErrorHandler[ErrorT, InputT, OutputT]): ErrorHandler[ErrorT, InputT, OutputT] = new ErrorHandler[ErrorT, InputT, OutputT] {
      override def tryRecover(startTime: UnixTimestamp, actualProcessStartTime: Option[UnixTimestamp], originalInput: InputT, processedInput: Option[InputT], error: ErrorT): IO[ErrorT, OutputT] = {
        value.tryRecover(
          startTime = startTime,
          actualProcessStartTime = actualProcessStartTime,
          originalInput = originalInput,
          processedInput = processedInput,
          error = error
        ).catchAll(newError => other.tryRecover(
          startTime = startTime,
          actualProcessStartTime = actualProcessStartTime,
          originalInput = originalInput,
          processedInput = processedInput,
          error = newError
        ))
      }
    }
  }
}
