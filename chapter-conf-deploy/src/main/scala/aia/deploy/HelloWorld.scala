package aia.deploy

import akka.actor.typed.scaladsl.Behaviors
import akka.actor.typed.{ActorRef, Behavior}

import scala.concurrent.duration._

object HelloWorld {
  final case class Envelope(msg: String, replyTo: ActorRef[String])

  def apply(): Behavior[Envelope] = Behaviors.setup { context =>
    Behaviors.receiveMessage { case Envelope(msg, replyTo) =>
      val hello = "Hello %s".format(msg)
      replyTo ! hello
      context.log.info(s"Sent response $hello")
      Behaviors.same
    }
  }
}

object HelloWorldCaller {
  sealed trait Command
  case class WrappedMessage(msg: String) extends Command
  case class TimerTick(msg: String)      extends Command

  def apply(duration: FiniteDuration, actor: ActorRef[HelloWorld.Envelope]): Behavior[Command] =
    Behaviors.setup[Command] { context =>
      Behaviors.withTimers { timer =>
        context.log.info("start")

        timer.startTimerWithFixedDelay(TimerTick("everybody"), duration)

        val helloResponseMapper: ActorRef[String] = context.messageAdapter(WrappedMessage.apply)
        Behaviors.receiveMessage {
          case TimerTick(msg) =>
            actor ! HelloWorld.Envelope(msg, helloResponseMapper)
            Behaviors.same
          case WrappedMessage(msg) =>
            context.log.info("received {}", msg)
            Behaviors.same
        }
      }
    }
}
