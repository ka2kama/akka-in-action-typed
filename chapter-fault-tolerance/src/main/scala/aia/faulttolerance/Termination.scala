package aia.faulttolerance

import akka.NotUsed
import akka.actor.typed.scaladsl.Behaviors
import akka.actor.typed.{ActorRef, Behavior, Terminated}

object DbStrategy2 {

  object DbWatcher {
    def apply(dbWriter: ActorRef[_]): Behavior[NotUsed] = Behaviors.setup { context =>
      context.watch(dbWriter)
      Behaviors.receiveSignal { case (_, Terminated(actorRef)) =>
        context.log.warn(s"Actor $actorRef terminated")
        Behaviors.same
      }
    }
  }
}
