package me.socure.extensible.processor

import me.socure.extensible.processor.model.UnixTimestamp
import scalaz.zio.IO

trait PostProcessor[ErrorT, InputT, OutputT] {
  def postProcess(
                   startTime: UnixTimestamp,
                   actualProcessStartTime: UnixTimestamp,
                   originalInput: InputT,
                   processedInput: InputT,
                   output: OutputT): IO[ErrorT, OutputT]
}

object PostProcessor {

  implicit class RichPostProcessor[ErrorT, InputT, OutputT](val value: PostProcessor[ErrorT, InputT, OutputT]) extends AnyVal {
    def and(other: PostProcessor[ErrorT, InputT, OutputT]): PostProcessor[ErrorT, InputT, OutputT] = new PostProcessor[ErrorT, InputT, OutputT] {
      override def postProcess(startTime: UnixTimestamp, actualProcessStartTime: UnixTimestamp, originalInput: InputT, processedInput: InputT, output: OutputT): IO[ErrorT, OutputT] = {
        for {
          originalResult <- value.postProcess(
            startTime = startTime,
            actualProcessStartTime = actualProcessStartTime,
            originalInput = originalInput,
            processedInput = processedInput,
            output = output
          )
          newResult <- other.postProcess(
            startTime = startTime,
            actualProcessStartTime = actualProcessStartTime,
            originalInput = originalInput,
            processedInput = processedInput,
            output = originalResult
          )
        } yield newResult
      }
    }

    def &(other: PostProcessor[ErrorT, InputT, OutputT]): PostProcessor[ErrorT, InputT, OutputT] = and(other)
  }

}
