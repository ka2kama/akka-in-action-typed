package aia.faulttolerance

import akka.actor.typed.scaladsl.Behaviors
import akka.actor.typed._

object LifeCycleHooks {

  sealed trait Command
  case object ForceRestart                                         extends Command
  case class SampleMessage(msg: String, replyTo: ActorRef[String]) extends Command

  private class ForceRestartException extends IllegalArgumentException("force restart")

  def apply(): Behavior[Command] = {
    Behaviors
      .supervise {
        Behaviors.setup[Command] { context =>
          context.log.info("preStart")
          Behaviors
            .receiveMessage[Command] {
              case ForceRestart =>
                throw new ForceRestartException
              case SampleMessage(msg, replyTo) =>
                context.log.info(s"Received: '$msg'. Sending back")
                replyTo ! msg
                Behaviors.same
            }
            .receiveSignal {
              case (_, PreRestart) =>
                context.log.info("preRestart")
                Behaviors.same
              case (_, PostStop) =>
                context.log.info("postStop")
                Behaviors.same
            }
        }
      }
      .onFailure(SupervisorStrategy.restart)
  }
}
