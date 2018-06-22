package me.socure.extensible.processor

import scalaz.zio.IO

trait Processor[ErrorT, InputT, OutputT] {
  def doProcess(input: InputT): IO[ErrorT, OutputT]
}

object Processor {

  implicit class RichProcessor[ErrorT, InputT, OutputT](val value: Processor[ErrorT, InputT, OutputT]) extends AnyVal {
    def withPreProcessors(preProcessors: Seq[PreProcessor[ErrorT, InputT]]): ExtensibleProcessor[ErrorT, InputT, OutputT] = {
      ExtensibleProcessor(
        preProcessors = preProcessors,
        processor = value,
        postProcessors = Seq.empty,
        errorHandlers = Seq.empty
      )
    }

    def withPostProcessors(postProcessors: Seq[PostProcessor[ErrorT, InputT, OutputT]]): ExtensibleProcessor[ErrorT, InputT, OutputT] = {
      ExtensibleProcessor(
        preProcessors = Seq.empty,
        processor = value,
        postProcessors = postProcessors,
        errorHandlers = Seq.empty
      )
    }

    def withErrorHandlers(errorHandlers: Seq[ErrorHandler[ErrorT, InputT, OutputT]]): ExtensibleProcessor[ErrorT, InputT, OutputT] = {
      ExtensibleProcessor(
        preProcessors = Seq.empty,
        processor = value,
        postProcessors = Seq.empty,
        errorHandlers = errorHandlers
      )
    }

    def withPreProcessor(preProcessor: PreProcessor[ErrorT, InputT]): ExtensibleProcessor[ErrorT, InputT, OutputT] = {
      ExtensibleProcessor(
        preProcessors = Seq(preProcessor),
        processor = value,
        postProcessors = Seq.empty,
        errorHandlers = Seq.empty
      )
    }

    def withPostProcessor(postProcessor: PostProcessor[ErrorT, InputT, OutputT]): ExtensibleProcessor[ErrorT, InputT, OutputT] = {
      ExtensibleProcessor(
        preProcessors = Seq.empty,
        processor = value,
        postProcessors = Seq(postProcessor),
        errorHandlers = Seq.empty
      )
    }

    def withErrorHandler(errorHandler: ErrorHandler[ErrorT, InputT, OutputT]): ExtensibleProcessor[ErrorT, InputT, OutputT] = {
      ExtensibleProcessor(
        preProcessors = Seq.empty,
        processor = value,
        postProcessors = Seq.empty,
        errorHandlers = Seq(errorHandler)
      )
    }
  }
}
