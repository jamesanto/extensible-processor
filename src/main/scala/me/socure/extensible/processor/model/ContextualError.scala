package me.socure.extensible.processor.model

case class ContextualError[ErrorT, CtxT](
                                          error: ErrorT,
                                          ctx: CtxT
                                        )
