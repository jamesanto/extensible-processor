package me.socure.extensible.processor

import me.socure.extensible.processor.model.UnixTimestamp
import scalaz.zio.IO

class ExtensibleProcessor[ErrorT, InputT, OutputT](
                                                    preProcessors: Seq[PreProcessor[ErrorT, InputT]],
                                                    processor: Processor[ErrorT, InputT, OutputT],
                                                    postProcessors: Seq[PostProcessor[ErrorT, InputT, OutputT]],
                                                    errorHandlers: Seq[ErrorHandler[ErrorT, InputT, OutputT]]
                                                  ) {
  private def tryRecover(
                          startTime: UnixTimestamp,
                          actualProcessStartTime: Option[UnixTimestamp],
                          originalInput: InputT,
                          processedInput: Option[InputT],
                          error: ErrorT,
                          remainingErrorHandlers: Seq[ErrorHandler[ErrorT, InputT, OutputT]]): IO[ErrorT, OutputT] = {
    if (remainingErrorHandlers.isEmpty) IO.fail(error) else {
      val resIO = remainingErrorHandlers.head.tryRecover(
        startTime = startTime,
        actualProcessStartTime = actualProcessStartTime,
        originalInput = originalInput,
        processedInput = processedInput,
        error = error
      )
      resIO.catchAll(newErr => tryRecover(
        startTime = startTime,
        actualProcessStartTime = actualProcessStartTime,
        originalInput = originalInput,
        processedInput = processedInput,
        error = newErr,
        remainingErrorHandlers = errorHandlers.tail
      ))
    }
  }

  private def handlePreProcessing(input: InputT, startTime: UnixTimestamp): IO[ErrorT, Either[InputT, OutputT]] = {
    val ppRes = preProcessors.foldLeft(IO.now[ErrorT, InputT](input)) { (res, current) =>
      for {
        r <- res
        newResult <- current.preProcess(r)
      } yield newResult
    }
    ppRes
      .map[Either[InputT, OutputT]](Left(_))
      .catchAll[ErrorT] { err =>
      val recoverd = tryRecover(
        startTime = startTime,
        actualProcessStartTime = None,
        originalInput = input,
        processedInput = None,
        error = err,
        remainingErrorHandlers = errorHandlers
      )
      recoverd.map(Right(_))
    }
  }

  private def handleProcessing(
                                input: InputT,
                                preProcessedInput: InputT,
                                startTime: UnixTimestamp,
                                actualProcessStartTime: UnixTimestamp
                              ): IO[ErrorT, OutputT] = {
    processor
      .doProcess(input = preProcessedInput)
      .catchAll(err => tryRecover(
        startTime = startTime,
        actualProcessStartTime = Some(actualProcessStartTime),
        originalInput = input,
        processedInput = Some(preProcessedInput),
        error = err,
        remainingErrorHandlers = errorHandlers
      ))
  }

  private def handlePostProcessing(input: InputT,
                                   preProcessedInput: InputT,
                                   startTime: UnixTimestamp,
                                   actualProcessStartTime: UnixTimestamp,
                                   processingResult: OutputT
                                  ): IO[ErrorT, OutputT] = {
    val ppRes = postProcessors.foldLeft(IO.now[ErrorT, OutputT](processingResult)) { (res, current) =>
      for {
        r <- res
        newResult <- current.postProcess(
          startTime = startTime,
          actualProcessStartTime = actualProcessStartTime,
          originalInput = input,
          processedInput = preProcessedInput,
          output = r
        )
      } yield newResult
    }
    ppRes.catchAll(err => tryRecover(
      startTime = startTime,
      actualProcessStartTime = Some(actualProcessStartTime),
      originalInput = input,
      processedInput = Some(preProcessedInput),
      error = err,
      remainingErrorHandlers = errorHandlers
    ))
  }

  def process(input: InputT): IO[ErrorT, OutputT] = {
    val startTime = UnixTimestamp.now()
    val result = for {
      preProcessedInputOrOutput <- handlePreProcessing(input = input, startTime = startTime)
      actualProcessStartTime = UnixTimestamp.now()
      finalResult <- {
        preProcessedInputOrOutput match {
          case Right(output) => IO.now[ErrorT, OutputT](output)
          case Left(preProcessedInput) =>
            for {
              processingResult <- handleProcessing(
                input = input,
                preProcessedInput = preProcessedInput,
                startTime = startTime,
                actualProcessStartTime = actualProcessStartTime
              )
              postProcessedResult <- handlePostProcessing(
                input = input,
                preProcessedInput = preProcessedInput,
                startTime = startTime,
                actualProcessStartTime = actualProcessStartTime,
                processingResult = processingResult
              )
            } yield postProcessedResult
        }
      }
    } yield finalResult
    result
  }

  def copy(
            preProcessors: Seq[PreProcessor[ErrorT, InputT]] = this.preProcessors,
            processor: Processor[ErrorT, InputT, OutputT] = this.processor,
            postProcessors: Seq[PostProcessor[ErrorT, InputT, OutputT]] = this.postProcessors,
            errorHandlers: Seq[ErrorHandler[ErrorT, InputT, OutputT]] = this.errorHandlers
          ): ExtensibleProcessor[ErrorT, InputT, OutputT] = {
    new ExtensibleProcessor[ErrorT, InputT, OutputT](
      preProcessors = preProcessors,
      processor = processor,
      postProcessors = postProcessors,
      errorHandlers = errorHandlers
    )
  }

  def withPreProcessors(
                         preProcessors: Seq[PreProcessor[ErrorT, InputT]]
                       ): ExtensibleProcessor[ErrorT, InputT, OutputT] = {
    new ExtensibleProcessor[ErrorT, InputT, OutputT](
      preProcessors = preProcessors ++ preProcessors,
      processor = processor,
      postProcessors = postProcessors,
      errorHandlers = errorHandlers
    )
  }

  def withPostProcessors(
                          postProcessors: Seq[PostProcessor[ErrorT, InputT, OutputT]]
                        ): ExtensibleProcessor[ErrorT, InputT, OutputT] = {
    new ExtensibleProcessor[ErrorT, InputT, OutputT](
      preProcessors = preProcessors,
      processor = processor,
      postProcessors = postProcessors ++ postProcessors,
      errorHandlers = errorHandlers
    )
  }

  def withErrorHandlers(
                         errorHandlers: Seq[ErrorHandler[ErrorT, InputT, OutputT]]
                       ): ExtensibleProcessor[ErrorT, InputT, OutputT] = {
    new ExtensibleProcessor[ErrorT, InputT, OutputT](
      preProcessors = preProcessors,
      processor = processor,
      postProcessors = postProcessors,
      errorHandlers = errorHandlers ++ errorHandlers
    )
  }

  def withPreProcessor(
                        preProcessor: PreProcessor[ErrorT, InputT]
                      ): ExtensibleProcessor[ErrorT, InputT, OutputT] = {
    new ExtensibleProcessor[ErrorT, InputT, OutputT](
      preProcessors = preProcessors :+ preProcessor,
      processor = processor,
      postProcessors = postProcessors,
      errorHandlers = errorHandlers
    )
  }

  def withPostProcessor(
                         postProcessor: PostProcessor[ErrorT, InputT, OutputT]
                       ): ExtensibleProcessor[ErrorT, InputT, OutputT] = {
    new ExtensibleProcessor[ErrorT, InputT, OutputT](
      preProcessors = preProcessors,
      processor = processor,
      postProcessors = postProcessors :+ postProcessor,
      errorHandlers = errorHandlers
    )
  }

  def withErrorHandler(
                        errorHandler: ErrorHandler[ErrorT, InputT, OutputT]
                      ): ExtensibleProcessor[ErrorT, InputT, OutputT] = {
    new ExtensibleProcessor[ErrorT, InputT, OutputT](
      preProcessors = preProcessors,
      processor = processor,
      postProcessors = postProcessors,
      errorHandlers = errorHandlers :+ errorHandler
    )
  }
}

object ExtensibleProcessor {
  def apply[ErrorT, InputT, OutputT](
                                      preProcessors: Seq[PreProcessor[ErrorT, InputT]],
                                      processor: Processor[ErrorT, InputT, OutputT],
                                      postProcessors: Seq[PostProcessor[ErrorT, InputT, OutputT]],
                                      errorHandlers: Seq[ErrorHandler[ErrorT, InputT, OutputT]]
                                    ): ExtensibleProcessor[ErrorT, InputT, OutputT] = {
    new ExtensibleProcessor[ErrorT, InputT, OutputT](
      preProcessors = preProcessors,
      processor = processor,
      postProcessors = postProcessors,
      errorHandlers = errorHandlers
    )
  }

  def apply[ErrorT, InputT, OutputT](
                                      preProcessor: PreProcessor[ErrorT, InputT],
                                      processor: Processor[ErrorT, InputT, OutputT],
                                      postProcessor: PostProcessor[ErrorT, InputT, OutputT],
                                      errorHandler: ErrorHandler[ErrorT, InputT, OutputT]
                                    ): ExtensibleProcessor[ErrorT, InputT, OutputT] = {
    apply(
      preProcessors = Seq(preProcessor),
      processor = processor,
      postProcessors = Seq(postProcessor),
      errorHandlers = Seq(errorHandler)
    )
  }

  def apply[ErrorT, InputT, OutputT](
                                      processor: Processor[ErrorT, InputT, OutputT]
                                    ): ExtensibleProcessor[ErrorT, InputT, OutputT] = {
    apply(
      preProcessors = Seq.empty,
      processor = processor,
      postProcessors = Seq.empty,
      errorHandlers = Seq.empty
    )
  }
}
