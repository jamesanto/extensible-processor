package me.socure.extensible.processor.model

case class ContextualOutput[OutputT, CtxT](
                                            output: OutputT,
                                            ctx: CtxT
                                          )
