package me.socure.extensible.processor.model

case class Message[HeaderValue, Body](
                                       headers: Map[String, HeaderValue],
                                       body: Body
                                     )
