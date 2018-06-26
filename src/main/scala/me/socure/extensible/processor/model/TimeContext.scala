package me.socure.extensible.processor.model

case class TimeContext(
                        preProcessingStartTime: UnixTimestamp,
                        preProcessingEndTime: Option[UnixTimestamp],
                        processingStartTime: Option[UnixTimestamp],
                        processingEndTime: Option[UnixTimestamp],
                        postProcessingStartTime: Option[UnixTimestamp],
                        postProcessingEndTime: Option[UnixTimestamp]
                      ) {
  override def toString: String = {
    s"preProcessingStartTime = [$preProcessingStartTime]\n" +
      s"preProcessingEndTime = [$preProcessingEndTime]\n" +
      s"processingStartTime = [$processingStartTime]\n" +
      s"processingEndTime = [$processingEndTime]\n" +
      s"postProcessingStartTime = [$postProcessingStartTime]\n" +
      s"postProcessingEndTime = [$postProcessingEndTime]\n"
  }
}

object TimeContext {
  def empty: TimeContext = TimeContext(
    preProcessingStartTime = UnixTimestamp.now(),
    preProcessingEndTime = None,
    processingStartTime = None,
    processingEndTime = None,
    postProcessingStartTime = None,
    postProcessingEndTime = None,
  )
}
