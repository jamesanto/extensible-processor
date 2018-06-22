package me.socure.extensible.processor.exp1

sealed trait LoginProcessingError

case object InvalidEmail extends LoginProcessingError

case object InvalidEmailNew extends LoginProcessingError

case object SomeLoginAuditorError extends LoginProcessingError

case object SomeLoginAuditorNewError extends LoginProcessingError

case object InvalidLogin extends LoginProcessingError
