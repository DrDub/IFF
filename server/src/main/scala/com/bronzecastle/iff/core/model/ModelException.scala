package com.bronzecastle.iff.core.model

object ModelException {
  class PreconditionFailedException extends Exception
  class UnableToPerformActionException extends PreconditionFailedException
  class ObjectDoesNotExistException extends PreconditionFailedException
  class ObjectNotAccessibleException extends PreconditionFailedException
  class ObjectNotMobileException extends PreconditionFailedException
  class ObjectTooBigException extends PreconditionFailedException

  class UnableToTravelException extends PreconditionFailedException
}