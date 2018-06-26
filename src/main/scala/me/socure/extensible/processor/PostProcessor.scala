package me.socure.extensible.processor

import me.socure.extensible.processor.model.{ContextualError, ContextualOutput}
import scalaz.zio.IO

trait PostProcessor[ErrorT, CtxT, InputT, OutputT] {
  def postProcess(
                   originalInput: InputT,
                   processedInput: InputT,
                   output: ContextualOutput[OutputT, CtxT]): IO[ContextualError[ErrorT, CtxT], ContextualOutput[OutputT, CtxT]]
}

object PostProcessor {

  implicit class RichPostProcessor[ErrorT, CtxT, InputT, OutputT](val value: PostProcessor[ErrorT, CtxT, InputT, OutputT]) extends AnyVal {
    def and(other: PostProcessor[ErrorT, CtxT, InputT, OutputT]): PostProcessor[ErrorT, CtxT, InputT, OutputT] = new PostProcessor[ErrorT, CtxT, InputT, OutputT] {
      override def postProcess(originalInput: InputT, processedInput: InputT, output: ContextualOutput[OutputT, CtxT]): IO[ContextualError[ErrorT, CtxT], ContextualOutput[OutputT, CtxT]] = {
        for {
          originalResult <- value.postProcess(
            originalInput = originalInput,
            processedInput = processedInput,
            output = output
          )
          newResult <- other.postProcess(
            originalInput = originalInput,
            processedInput = processedInput,
            output = originalResult
          )
        } yield newResult
      }
    }

    def &(other: PostProcessor[ErrorT, CtxT, InputT, OutputT]): PostProcessor[ErrorT, CtxT, InputT, OutputT] = and(other)
  }

}
