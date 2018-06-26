package me.socure.extensible.processor

import me.socure.extensible.processor.model.{ContextualError, ContextualOutput}
import scalaz.zio.IO

trait Processor[ErrorT, CtxT, InputT, OutputT] {
  def process(input: InputT, ctx: CtxT): IO[ContextualError[ErrorT, CtxT], ContextualOutput[OutputT, CtxT]]
}

object Processor {

  implicit class RichProcessor[ErrorT, CtxT, InputT, OutputT](val value: Processor[ErrorT, CtxT, InputT, OutputT]) extends AnyVal {
    def withPreProcessors(preProcessors: Seq[PreProcessor[ErrorT, InputT]]): ExtensibleProcessor[ErrorT, CtxT, InputT, OutputT] = {
      ExtensibleProcessor(
        preProcessors = preProcessors,
        processor = value,
        postProcessors = Seq.empty,
        errorHandlers = Seq.empty,
        contextProcessors = Seq.empty
      )
    }

    def withPostProcessors(postProcessors: Seq[PostProcessor[ErrorT, CtxT, InputT, OutputT]]): ExtensibleProcessor[ErrorT, CtxT, InputT, OutputT] = {
      ExtensibleProcessor(
        preProcessors = Seq.empty,
        processor = value,
        postProcessors = postProcessors,
        errorHandlers = Seq.empty,
        contextProcessors = Seq.empty
      )
    }

    def withErrorHandlers(errorHandlers: Seq[ErrorHandler[ErrorT, CtxT, InputT, OutputT]]): ExtensibleProcessor[ErrorT, CtxT, InputT, OutputT] = {
      ExtensibleProcessor(
        preProcessors = Seq.empty,
        processor = value,
        postProcessors = Seq.empty,
        errorHandlers = errorHandlers,
        contextProcessors = Seq.empty
      )
    }

    def withContextProcessors(contextProcessors: Seq[ContextProcessor[ErrorT, CtxT]]): ExtensibleProcessor[ErrorT, CtxT, InputT, OutputT] = {
      ExtensibleProcessor(
        preProcessors = Seq.empty,
        processor = value,
        postProcessors = Seq.empty,
        errorHandlers = Seq.empty,
        contextProcessors = contextProcessors
      )
    }

    def withPreProcessor(preProcessor: PreProcessor[ErrorT, InputT]): ExtensibleProcessor[ErrorT, CtxT, InputT, OutputT] = {
      ExtensibleProcessor(
        preProcessors = Seq(preProcessor),
        processor = value,
        postProcessors = Seq.empty,
        errorHandlers = Seq.empty,
        contextProcessors = Seq.empty
      )
    }

    def withPostProcessor(postProcessor: PostProcessor[ErrorT, CtxT, InputT, OutputT]): ExtensibleProcessor[ErrorT, CtxT, InputT, OutputT] = {
      ExtensibleProcessor(
        preProcessors = Seq.empty,
        processor = value,
        postProcessors = Seq(postProcessor),
        errorHandlers = Seq.empty,
        contextProcessors = Seq.empty
      )
    }

    def withErrorHandler(errorHandler: ErrorHandler[ErrorT, CtxT, InputT, OutputT]): ExtensibleProcessor[ErrorT, CtxT, InputT, OutputT] = {
      ExtensibleProcessor(
        preProcessors = Seq.empty,
        processor = value,
        postProcessors = Seq.empty,
        errorHandlers = Seq(errorHandler),
        contextProcessors = Seq.empty
      )
    }

    def withContextProcessor(contextProcessor: ContextProcessor[ErrorT, CtxT]): ExtensibleProcessor[ErrorT, CtxT, InputT, OutputT] = {
      ExtensibleProcessor(
        preProcessors = Seq.empty,
        processor = value,
        postProcessors = Seq.empty,
        errorHandlers = Seq.empty,
        contextProcessors = Seq(contextProcessor)
      )
    }
  }
}
