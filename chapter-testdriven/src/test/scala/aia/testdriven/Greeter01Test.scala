package aia.testdriven
import akka.actor.ActorSystem
import akka.actor.testkit.typed.scaladsl.ScalaTestWithActorTestKit
import akka.actor.typed.DispatcherSelector
import akka.testkit.{CallingThreadDispatcher, EventFilter}
import com.typesafe.config.ConfigFactory
import org.scalatest.wordspec.AnyWordSpecLike

class Greeter01Test extends ScalaTestWithActorTestKit with AnyWordSpecLike {

  "The Greeter" should {
    "say Hello World! when a Greeting(\"World\") is sent to it" in {

      val dispatcherId = CallingThreadDispatcher.Id
      val behavior     = Greeter()
      val greeter      = spawn(behavior, DispatcherSelector.fromConfig(dispatcherId))
//      EventFilter
//        .info(message = "Hello World!", occurrences = 1)
//        .intercept {
//          greeter ! Greeting("World")
//        }(system.classicSystem)
    }
  }
}

object Greeter01Test {
  val testSystem: ActorSystem = {
    val config =
      ConfigFactory.parseString("""
         akka.loggers = [akka.testkit.TestEventListener]
      """)
    ActorSystem("testsystem", config)
  }
}
