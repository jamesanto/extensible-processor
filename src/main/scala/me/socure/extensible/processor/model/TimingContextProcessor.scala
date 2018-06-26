package me.socure.extensible.processor.model

import me.socure.extensible.processor.ContextProcessor
import scalaz.zio.IO


class TimingContextProcessor[ErrorT] extends ContextProcessor[ErrorT, TimeContext] {
  override def prePreProcessing(ctx: TimeContext): IO[ErrorT, TimeContext] = {
    IO.now(ctx.copy(
      preProcessingStartTime = UnixTimestamp.now()
    ))
  }

  override def postPreProcessing(ctx: TimeContext): IO[ErrorT, TimeContext] = {
    IO.now(ctx.copy(
      preProcessingEndTime = Some(UnixTimestamp.now())
    ))
  }

  override def preProcessing(ctx: TimeContext): IO[ErrorT, TimeContext] = {
    IO.now(ctx.copy(
      processingStartTime = Some(UnixTimestamp.now())
    ))
  }

  override def postProcessing(ctx: TimeContext): IO[ErrorT, TimeContext] = {
    IO.now(ctx.copy(
      processingEndTime = Some(UnixTimestamp.now())
    ))
  }

  override def prePostProcessing(ctx: TimeContext): IO[ErrorT, TimeContext] = {
    IO.now(ctx.copy(
      postProcessingStartTime = Some(UnixTimestamp.now())
    ))
  }

  override def postPostProcessing(ctx: TimeContext): IO[ErrorT, TimeContext] = {
    IO.now(ctx.copy(
      postProcessingEndTime = Some(UnixTimestamp.now())
    ))
  }
}

object TimingContextProcessor {
  def apply[ErrorT]: TimingContextProcessor[ErrorT] = new TimingContextProcessor[ErrorT]
}
