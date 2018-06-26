package me.socure.extensible.processor

import me.socure.extensible.processor.model.{ContextualError, ContextualOutput}
import scalaz.zio.IO

trait ErrorHandler[ErrorT, CtxT, InputT, OutputT] {
  def tryRecover(
                  originalInput: InputT,
                  processedInput: Option[InputT],
                  originalCtx: CtxT,
                  processedCtx: CtxT,
                  error: ErrorT): IO[ContextualError[ErrorT, CtxT], ContextualOutput[OutputT, CtxT]]
}

object ErrorHandler {

  implicit class RichErrorHandler[ErrorT, CtxT, InputT, OutputT](val value: ErrorHandler[ErrorT, CtxT, InputT, OutputT]) extends AnyVal {
    def and(other: ErrorHandler[ErrorT, CtxT, InputT, OutputT]): ErrorHandler[ErrorT, CtxT, InputT, OutputT] = new ErrorHandler[ErrorT, CtxT, InputT, OutputT] {
      override def tryRecover(
                               originalInput: InputT,
                               processedInput: Option[InputT],
                               originalCtx: CtxT,
                               processedCtx: CtxT,
                               error: ErrorT): IO[ContextualError[ErrorT, CtxT], ContextualOutput[OutputT, CtxT]] = {
        value.tryRecover(
          originalInput = originalInput,
          processedInput = processedInput,
          originalCtx = originalCtx,
          processedCtx = processedCtx,
          error = error
        ).catchAll(newError => other.tryRecover(
          originalInput = originalInput,
          processedInput = processedInput,
          originalCtx = originalCtx,
          processedCtx = newError.ctx,
          error = newError.error
        ))
      }
    }
  }
}
