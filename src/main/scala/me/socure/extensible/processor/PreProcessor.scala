package me.socure.extensible.processor

import scalaz.zio.IO

trait PreProcessor[ErrorT, InputT] {
  def preProcess(input: InputT): IO[ErrorT, InputT]
}

object PreProcessor {

  implicit class RichPreProcessor[ErrorT, InputT](val value: PreProcessor[ErrorT, InputT]) extends AnyVal {
    def and(other: PreProcessor[ErrorT, InputT]): PreProcessor[ErrorT, InputT] = new PreProcessor[ErrorT, InputT] {
      override def preProcess(input: InputT): IO[ErrorT, InputT] = {
        for {
          originalResult <- value.preProcess(input)
          newResult <- other.preProcess(originalResult)
        } yield newResult
      }
    }

    def &(other: PreProcessor[ErrorT, InputT]): PreProcessor[ErrorT, InputT] = and(other)
  }
}
