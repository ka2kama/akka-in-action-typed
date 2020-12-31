package aia.testdriven

import akka.actor.testkit.typed.scaladsl.ScalaTestWithActorTestKit
import akka.actor.typed.scaladsl.{ActorContext, Behaviors}
import akka.actor.typed.{ActorRef, Behavior}
import org.scalatest.wordspec.AnyWordSpecLike

class Greeter02Test extends ScalaTestWithActorTestKit with AnyWordSpecLike {

  "The Greeter" should {
    "say Hello World! when a Greeting(\"World\") is sent to it" in {
      val probe    = createTestProbe[String]()
      val behavior = Greeter02(Some(probe.ref))
      val greeter  = spawn(behavior, "greeter02-1")
      greeter ! Greeting("World")
      probe.expectMessage("Hello World!")
    }
    "say something else and see what happens" in {
      val probe    = createTestProbe[String]()
      val behavior = Greeter02(Some(probe.ref))
      val greeter  = spawn(behavior, "greeter02-2")
//      system.eventStream.subscribe(testActor, classOf[UnhandledMessage])
//      greeter ! "World"
//      expectMsg(UnhandledMessage("World", system.deadLetters, greeter))
    }
  }
}

object Greeter02 {
  def apply(listener: Option[ActorRef[String]] = None): Behavior[Greeting] =
    Behaviors.setup { context =>
      new Greeter02(context, listener).init()
    }
}
class Greeter02 private (context: ActorContext[Greeting], listener: Option[ActorRef[String]]) {
  def init(): Behavior[Greeting] = Behaviors.receiveMessage { case Greeting(who) =>
    val message = s"Hello $who!"
    context.log.info(message)
    listener.foreach(_ ! message)
    Behaviors.same
  }
}
