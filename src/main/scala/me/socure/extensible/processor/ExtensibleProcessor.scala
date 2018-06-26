package me.socure.extensible.processor

import me.socure.extensible.processor.model.{ContextualError, ContextualOutput}
import scalaz.zio.IO

class ExtensibleProcessor[ErrorT, CtxT, InputT, OutputT](
                                                    preProcessors: Seq[PreProcessor[ErrorT, InputT]],
                                                    processor: Processor[ErrorT, CtxT, InputT, OutputT],
                                                    postProcessors: Seq[PostProcessor[ErrorT, CtxT, InputT, OutputT]],
                                                    errorHandlers: Seq[ErrorHandler[ErrorT, CtxT, InputT, OutputT]],
                                                    contextProcessors: Seq[ContextProcessor[ErrorT, CtxT]]
                                                  ) extends Processor[ErrorT, CtxT, InputT, OutputT] {
  private def tryRecover(
                          originalInput: InputT,
                          processedInput: Option[InputT],
                          originalCtx: CtxT,
                          processedCtx: CtxT,
                          error: ErrorT,
                          remainingErrorHandlers: Seq[ErrorHandler[ErrorT, CtxT, InputT, OutputT]]): IO[ContextualError[ErrorT, CtxT], ContextualOutput[OutputT, CtxT]] = {
    if (remainingErrorHandlers.isEmpty) IO.fail(ContextualError(error = error, ctx = processedCtx)) else {
      val resIO = remainingErrorHandlers.head.tryRecover(
        originalInput = originalInput,
        processedInput = processedInput,
        originalCtx = originalCtx,
        processedCtx = processedCtx,
        error = error
      )
      resIO.catchAll(newErr => tryRecover(
        originalInput = originalInput,
        processedInput = processedInput,
        originalCtx = originalCtx,
        processedCtx = newErr.ctx,
        error = newErr.error,
        remainingErrorHandlers = errorHandlers.tail
      ))
    }
  }

  private def handlePreProcessing(input: InputT, originalCtx: CtxT, processedCtx: CtxT): IO[ContextualError[ErrorT, CtxT], Either[InputT, ContextualOutput[OutputT, CtxT]]] = {
    val ppRes = preProcessors.foldLeft(IO.now[ErrorT, InputT](input)) { (res, current) =>
      for {
        r <- res
        newResult <- current.preProcess(r)
      } yield newResult
    }
    ppRes
      .map[Either[InputT, ContextualOutput[OutputT, CtxT]]](Left(_))
      .catchAll[ContextualError[ErrorT, CtxT]] { err =>
      val recoverd = tryRecover(
        originalInput = input,
        processedInput = None,
        originalCtx = originalCtx,
        processedCtx = processedCtx,
        error = err,
        remainingErrorHandlers = errorHandlers
      )
      recoverd.map(Right(_))
    }
  }

  private def handleProcessing(
                                input: InputT,
                                originalCtx: CtxT,
                                processedCtx: CtxT,
                                preProcessedInput: InputT
                              ): IO[ContextualError[ErrorT, CtxT], ContextualOutput[OutputT, CtxT]] = {
    processor
      .process(input = preProcessedInput, ctx = processedCtx)
      .catchAll(err => tryRecover(
        originalInput = input,
        processedInput = Some(preProcessedInput),
        originalCtx = originalCtx,
        processedCtx = err.ctx,
        error = err.error,
        remainingErrorHandlers = errorHandlers
      ))
  }

  private def handlePostProcessing(input: InputT,
                                   preProcessedInput: InputT,
                                   originalCtx: CtxT,
                                   processedCtx: CtxT,
                                   processingResult: ContextualOutput[OutputT, CtxT]
                                  ): IO[ContextualError[ErrorT, CtxT], ContextualOutput[OutputT, CtxT]] = {
    val ppRes = postProcessors.foldLeft(IO.now[ContextualError[ErrorT, CtxT], ContextualOutput[OutputT, CtxT]](processingResult)) { (res, current) =>
      for {
        r <- res
        newResult <- current.postProcess(
          originalInput = input,
          processedInput = preProcessedInput,
          output = r
        )
      } yield newResult
    }
    ppRes.catchAll(err => tryRecover(
      originalInput = input,
      processedInput = Some(preProcessedInput),
      originalCtx = originalCtx,
      processedCtx = err.ctx,
      error = err.error,
      remainingErrorHandlers = errorHandlers
    ))
  }

  private def handleContext(ctx: CtxT)(stage: ContextProcessor[ErrorT, CtxT] => CtxT => IO[ErrorT, CtxT]): IO[ErrorT, CtxT] = {
    contextProcessors.foldLeft(IO.now[ErrorT, CtxT](ctx)) { (res, current) =>
      for {
        r <- res
        newResult <- stage(current)(r)
      } yield newResult
    }
  }

  def process(input: InputT, ctx: CtxT): IO[ContextualError[ErrorT, CtxT], ContextualOutput[OutputT, CtxT]] = {
    val result: IO[ContextualError[ErrorT, CtxT], ContextualOutput[OutputT, CtxT]] = for {
      ctxPrePreProcessing <- handleContext(ctx)(_.prePreProcessing).leftMap(error => ContextualError(error = error, ctx = ctx))
      preProcessedInputOrOutput <- handlePreProcessing(input = input, originalCtx = ctx, processedCtx = ctxPrePreProcessing)
      ctxPostPreProcessing <- handleContext(preProcessedInputOrOutput.right.map(_.ctx).getOrElse(ctxPrePreProcessing))(_.postPreProcessing).leftMap(error => ContextualError(error = error, ctx = preProcessedInputOrOutput.right.map(_.ctx).getOrElse(ctxPrePreProcessing)))
      finalResult <- {
        preProcessedInputOrOutput match {
          case Right(output) => IO.now[ContextualError[ErrorT, CtxT], ContextualOutput[OutputT, CtxT]](output)
          case Left(preProcessedInput) =>
            for {
              ctxPreProcessing <- handleContext(ctxPostPreProcessing)(_.preProcessing).leftMap(error => ContextualError(error = error, ctx = ctxPostPreProcessing))
              processingResult <- handleProcessing(
                input = input,
                preProcessedInput = preProcessedInput,
                originalCtx = ctx,
                processedCtx = ctxPreProcessing
              )
              ctxPostProcessing <- handleContext(processingResult.ctx)(_.postProcessing).leftMap(error => ContextualError(error = error, ctx = processingResult.ctx))
              ctxPrePostProcessing <- handleContext(ctxPostProcessing)(_.prePostProcessing).leftMap(error => ContextualError(error = error, ctx = ctxPostProcessing))
              postProcessedResult <- handlePostProcessing(
                input = input,
                preProcessedInput = preProcessedInput,
                originalCtx = ctx,
                processedCtx = ctxPrePostProcessing,
                processingResult = processingResult.copy(ctx = ctxPrePostProcessing)
              )
              ctxPostPostProcessing <- handleContext(ctxPrePostProcessing)(_.postPostProcessing).leftMap(error => ContextualError(error = error, ctx = ctxPrePostProcessing))
            } yield postProcessedResult.copy(ctx = ctxPostPostProcessing)
        }
      }
    } yield finalResult
    result
  }

  def copy(
            preProcessors: Seq[PreProcessor[ErrorT, InputT]] = this.preProcessors,
            processor: Processor[ErrorT, CtxT, InputT, OutputT] = this.processor,
            postProcessors: Seq[PostProcessor[ErrorT, CtxT, InputT, OutputT]] = this.postProcessors,
            errorHandlers: Seq[ErrorHandler[ErrorT, CtxT, InputT, OutputT]] = this.errorHandlers,
            contextProcessors: Seq[ContextProcessor[ErrorT, CtxT]] = this.contextProcessors
          ): ExtensibleProcessor[ErrorT, CtxT, InputT, OutputT] = {
    new ExtensibleProcessor[ErrorT, CtxT, InputT, OutputT](
      preProcessors = preProcessors,
      processor = processor,
      postProcessors = postProcessors,
      errorHandlers = errorHandlers,
      contextProcessors = contextProcessors
    )
  }

  def withPreProcessors(
                         preProcessors: Seq[PreProcessor[ErrorT, InputT]]
                       ): ExtensibleProcessor[ErrorT, CtxT, InputT, OutputT] = {
    new ExtensibleProcessor[ErrorT, CtxT, InputT, OutputT](
      preProcessors = this.preProcessors ++ preProcessors,
      processor = processor,
      postProcessors = postProcessors,
      errorHandlers = errorHandlers,
      contextProcessors = contextProcessors
    )
  }

  def withPostProcessors(
                          postProcessors: Seq[PostProcessor[ErrorT, CtxT, InputT, OutputT]]
                        ): ExtensibleProcessor[ErrorT, CtxT, InputT, OutputT] = {
    new ExtensibleProcessor[ErrorT, CtxT, InputT, OutputT](
      preProcessors = preProcessors,
      processor = processor,
      postProcessors = this.postProcessors ++ postProcessors,
      errorHandlers = errorHandlers,
      contextProcessors = contextProcessors
    )
  }

  def withErrorHandlers(
                         errorHandlers: Seq[ErrorHandler[ErrorT, CtxT, InputT, OutputT]]
                       ): ExtensibleProcessor[ErrorT, CtxT, InputT, OutputT] = {
    new ExtensibleProcessor[ErrorT, CtxT, InputT, OutputT](
      preProcessors = preProcessors,
      processor = processor,
      postProcessors = postProcessors,
      errorHandlers = this.errorHandlers ++ errorHandlers,
      contextProcessors = contextProcessors
    )
  }

  def withContextProcessors(
                         contextProcessors: Seq[ContextProcessor[ErrorT, CtxT]]
                       ): ExtensibleProcessor[ErrorT, CtxT, InputT, OutputT] = {
    new ExtensibleProcessor[ErrorT, CtxT, InputT, OutputT](
      preProcessors = preProcessors,
      processor = processor,
      postProcessors = postProcessors,
      errorHandlers = errorHandlers,
      contextProcessors = this.contextProcessors ++ contextProcessors
    )
  }

  def withPreProcessor(
                        preProcessor: PreProcessor[ErrorT, InputT]
                      ): ExtensibleProcessor[ErrorT, CtxT, InputT, OutputT] = {
    new ExtensibleProcessor[ErrorT, CtxT, InputT, OutputT](
      preProcessors = preProcessors :+ preProcessor,
      processor = processor,
      postProcessors = postProcessors,
      errorHandlers = errorHandlers,
      contextProcessors = contextProcessors
    )
  }

  def withPostProcessor(
                         postProcessor: PostProcessor[ErrorT, CtxT, InputT, OutputT]
                       ): ExtensibleProcessor[ErrorT, CtxT, InputT, OutputT] = {
    new ExtensibleProcessor[ErrorT, CtxT, InputT, OutputT](
      preProcessors = preProcessors,
      processor = processor,
      postProcessors = postProcessors :+ postProcessor,
      errorHandlers = errorHandlers,
      contextProcessors = contextProcessors
    )
  }

  def withErrorHandler(
                        errorHandler: ErrorHandler[ErrorT, CtxT, InputT, OutputT]
                      ): ExtensibleProcessor[ErrorT, CtxT, InputT, OutputT] = {
    new ExtensibleProcessor[ErrorT, CtxT, InputT, OutputT](
      preProcessors = preProcessors,
      processor = processor,
      postProcessors = postProcessors,
      errorHandlers = errorHandlers :+ errorHandler,
      contextProcessors = contextProcessors
    )
  }

  def withContextProcessor(
                        contextProcessor: ContextProcessor[ErrorT, CtxT]
                      ): ExtensibleProcessor[ErrorT, CtxT, InputT, OutputT] = {
    new ExtensibleProcessor[ErrorT, CtxT, InputT, OutputT](
      preProcessors = preProcessors,
      processor = processor,
      postProcessors = postProcessors,
      errorHandlers = errorHandlers,
      contextProcessors = contextProcessors :+ contextProcessor
    )
  }
}

object ExtensibleProcessor {
  def apply[ErrorT, CtxT, InputT, OutputT](
                                      preProcessors: Seq[PreProcessor[ErrorT, InputT]],
                                      processor: Processor[ErrorT, CtxT, InputT, OutputT],
                                      postProcessors: Seq[PostProcessor[ErrorT, CtxT, InputT, OutputT]],
                                      errorHandlers: Seq[ErrorHandler[ErrorT, CtxT, InputT, OutputT]],
                                      contextProcessors: Seq[ContextProcessor[ErrorT, CtxT]]
                                    ): ExtensibleProcessor[ErrorT, CtxT, InputT, OutputT] = {
    new ExtensibleProcessor[ErrorT, CtxT, InputT, OutputT](
      preProcessors = preProcessors,
      processor = processor,
      postProcessors = postProcessors,
      errorHandlers = errorHandlers,
      contextProcessors = contextProcessors
    )
  }

  def apply[ErrorT, CtxT, InputT, OutputT](
                                      preProcessor: PreProcessor[ErrorT, InputT],
                                      processor: Processor[ErrorT, CtxT, InputT, OutputT],
                                      postProcessor: PostProcessor[ErrorT, CtxT, InputT, OutputT],
                                      errorHandler: ErrorHandler[ErrorT, CtxT, InputT, OutputT],
                                      contextProcessor: ContextProcessor[ErrorT, CtxT]
                                    ): ExtensibleProcessor[ErrorT, CtxT, InputT, OutputT] = {
    apply(
      preProcessors = Seq(preProcessor),
      processor = processor,
      postProcessors = Seq(postProcessor),
      errorHandlers = Seq(errorHandler),
      contextProcessors = Seq(contextProcessor)
    )
  }

  def apply[ErrorT, CtxT, InputT, OutputT](
                                      processor: Processor[ErrorT, CtxT, InputT, OutputT]
                                    ): ExtensibleProcessor[ErrorT, CtxT, InputT, OutputT] = {
    apply(
      preProcessors = Seq.empty,
      processor = processor,
      postProcessors = Seq.empty,
      errorHandlers = Seq.empty,
      contextProcessors = Seq.empty
    )
  }
}
