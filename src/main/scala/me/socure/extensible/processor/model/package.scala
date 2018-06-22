package me.socure.extensible.processor

import java.io.InputStream

import org.json4s.JsonAST.JValue

package object model {
  type StringHeaderMessage[Body] = Message[String, Body]
  type JsonHeaderMessage[Body] = Message[JValue, Body]
  type JsonBodyMessage[HeaderValue] = Message[HeaderValue, JValue]
  type JsonHeaderBodyMessage = Message[JValue, JValue]
  type StringHeaderJsonBodyMessage = Message[String, JValue]
  type StringHeaderStreamBodyMessage = Message[String, InputStream]
  type StringHeaderBinaryBodyMessage = Message[String, Array[Byte]]
}
