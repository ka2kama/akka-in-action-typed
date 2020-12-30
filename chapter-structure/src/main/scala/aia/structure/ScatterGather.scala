package aia.structure

import akka.actor.typed.scaladsl.{ActorContext, Behaviors}
import akka.actor.typed.{ActorRef, Behavior, PreRestart, SupervisorStrategy}

import java.text.SimpleDateFormat
import java.util.Date
import scala.concurrent.duration.FiniteDuration

sealed trait PhotoCommand

final case class PhotoMessage(
    id: String,
    photo: String,
    creationTime: Option[Date] = None,
    speed: Option[Int] = None,
) extends PhotoCommand

final case class TimeoutMessage(msg: PhotoMessage) extends PhotoCommand
final case class Exception(ex: Throwable)          extends PhotoCommand

object ImageProcessing {
  val dateFormat = new SimpleDateFormat("ddMMyyyy HH:mm:ss.SSS")
  def getSpeed(image: String): Option[Int] = {
    val attributes = image.split('|')
    if (attributes.length == 3)
      Some(attributes(1).toInt)
    else
      None
  }
  def getTime(image: String): Option[Date] = {
    val attributes = image.split('|')
    if (attributes.length == 3)
      Some(dateFormat.parse(attributes(0)))
    else
      None
  }
  def getLicense(image: String): Option[String] = {
    val attributes = image.split('|')
    if (attributes.length == 3)
      Some(attributes(2))
    else
      None
  }
  def createPhotoString(date: Date, speed: Int): String = {
    createPhotoString(date, speed, " ")
  }

  def createPhotoString(date: Date, speed: Int, license: String): String = {
    "%s|%s|%s".format(dateFormat.format(date), speed, license)
  }
}

object GetSpeed {
  def apply(pipe: ActorRef[PhotoMessage]): Behavior[PhotoMessage] = Behaviors.receiveMessage {
    msg =>
      pipe ! msg.copy(speed = ImageProcessing.getSpeed(msg.photo))

      Behaviors.same
  }
}

object GetTime {
  def apply(pipe: ActorRef[PhotoMessage]): Behavior[PhotoMessage] = Behaviors.receiveMessage {
    msg =>
      pipe ! msg.copy(creationTime = ImageProcessing.getTime(msg.photo))

      Behaviors.same
  }
}

object RecipientList {
  def apply[T](recipientList: Seq[ActorRef[T]]): Behavior[T] =
    Behaviors.receiveMessage { msg =>
      recipientList.foreach(_ ! msg)
      Behaviors.same
    }
}

object Aggregator {

  def apply(timeout: FiniteDuration, pipe: ActorRef[PhotoCommand]): Behavior[PhotoCommand] =
    Behaviors
      .supervise[PhotoCommand] {
        Behaviors.setup { context =>
          new Aggregator(context, timeout, pipe).init()
        }
      }
      .onFailure(SupervisorStrategy.restart)
}

class Aggregator private (
    context: ActorContext[PhotoCommand],
    timeout: FiniteDuration,
    pipe: ActorRef[PhotoCommand],
) {

  def init(): Behavior[PhotoCommand] = next(Map.empty)

  def next(messages: Map[String, PhotoMessage]): Behavior[PhotoCommand] =
    Behaviors
      .receiveMessage[PhotoCommand] {
        case rcvMsg: PhotoMessage =>
          messages.get(rcvMsg.id) match {
            case Some(alreadyRcvMsg) =>
              val newCombinedMsg = PhotoMessage(
                rcvMsg.id,
                rcvMsg.photo,
                rcvMsg.creationTime.orElse(alreadyRcvMsg.creationTime),
                rcvMsg.speed.orElse(alreadyRcvMsg.speed),
              )
              pipe ! newCombinedMsg
              //cleanup message
              next(messages - alreadyRcvMsg.id)
            case None =>
              context.scheduleOnce(timeout, context.self, TimeoutMessage(rcvMsg))
              next(messages + (rcvMsg.id -> rcvMsg))
          }
        case TimeoutMessage(rcvMsg) =>
          messages.get(rcvMsg.id) match {
            case Some(alreadyRcvMsg) =>
              pipe ! alreadyRcvMsg
              next(messages - alreadyRcvMsg.id)
            case None => //message is already processed
              Behaviors.same
          }

        case Exception(ex) => throw ex
      }
      .receiveSignal { case (_, PreRestart) =>
        messages.values.foreach(context.self ! _)
        init()
      }
}
