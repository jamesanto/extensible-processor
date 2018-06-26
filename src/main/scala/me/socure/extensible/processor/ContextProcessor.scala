package me.socure.extensible.processor

import scalaz.zio.IO

trait ContextProcessor[ErrorT, CtxT] {
  def prePreProcessing(ctx: CtxT): IO[ErrorT, CtxT] = IO.now(ctx)

  def postPreProcessing(ctx: CtxT): IO[ErrorT, CtxT] = IO.now(ctx)

  def preProcessing(ctx: CtxT): IO[ErrorT, CtxT] = IO.now(ctx)

  def postProcessing(ctx: CtxT): IO[ErrorT, CtxT] = IO.now(ctx)

  def prePostProcessing(ctx: CtxT): IO[ErrorT, CtxT] = IO.now(ctx)

  def postPostProcessing(ctx: CtxT): IO[ErrorT, CtxT] = IO.now(ctx)
}

object ContextProcessor {

  implicit class RichContextProcessor[ErrorT, CtxT](val value: ContextProcessor[ErrorT, CtxT]) extends AnyVal {
    def and(other: ContextProcessor[ErrorT, CtxT]): ContextProcessor[ErrorT, CtxT] = new ContextProcessor[ErrorT, CtxT] {
      override def prePreProcessing(ctx: CtxT): IO[ErrorT, CtxT] = {
        for {
          originalResult <- value.prePreProcessing(ctx)
          newResult <- other.prePreProcessing(originalResult)
        } yield newResult
      }

      override def postPreProcessing(ctx: CtxT): IO[ErrorT, CtxT] = {
        for {
          originalResult <- value.postPreProcessing(ctx)
          newResult <- other.postPreProcessing(originalResult)
        } yield newResult
      }

      override def preProcessing(ctx: CtxT): IO[ErrorT, CtxT] = {
        for {
          originalResult <- value.preProcessing(ctx)
          newResult <- other.preProcessing(originalResult)
        } yield newResult
      }

      override def postProcessing(ctx: CtxT): IO[ErrorT, CtxT] = {
        for {
          originalResult <- value.postProcessing(ctx)
          newResult <- other.postProcessing(originalResult)
        } yield newResult
      }

      override def prePostProcessing(ctx: CtxT): IO[ErrorT, CtxT] = {
        for {
          originalResult <- value.prePostProcessing(ctx)
          newResult <- other.prePostProcessing(originalResult)
        } yield newResult
      }

      override def postPostProcessing(ctx: CtxT): IO[ErrorT, CtxT] = {
        for {
          originalResult <- value.postPostProcessing(ctx)
          newResult <- other.postPostProcessing(originalResult)
        } yield newResult
      }
    }

    def &(other: ContextProcessor[ErrorT, CtxT]): ContextProcessor[ErrorT, CtxT] = and(other)
  }

}
