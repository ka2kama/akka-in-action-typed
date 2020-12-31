package aia.testdriven

import akka.actor.typed.Behavior
import akka.actor.typed.scaladsl.Behaviors

case class Greeting(message: String)

object Greeter {
  def apply(): Behavior[Greeting] = Behaviors.setup { context =>
    Behaviors.receiveMessage { case Greeting(message) =>
      context.log.info(s"Hello $message!")
      Behaviors.same
    }
  }
}
