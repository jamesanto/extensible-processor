package me.socure.extensible.processor.model

case class UnixTimestamp(value: Long) extends AnyVal

object UnixTimestamp {
  def now(): UnixTimestamp = UnixTimestamp(value = System.currentTimeMillis())
}
