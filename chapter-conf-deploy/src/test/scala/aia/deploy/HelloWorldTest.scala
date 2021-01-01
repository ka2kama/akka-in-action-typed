package aia.deploy

import akka.actor.testkit.typed.scaladsl.ScalaTestWithActorTestKit
import akka.actor.typed.ActorRef
import org.scalatest.wordspec.AnyWordSpecLike

class HelloWorldTest extends ScalaTestWithActorTestKit with AnyWordSpecLike {

  val actor: ActorRef[HelloWorld.Envelope] = spawn(HelloWorld())

  "HelloWorld" should {
    "reply when sending a string" in {
      val probe = createTestProbe[HelloWorldCaller.Message]()
      actor ! HelloWorld.Envelope("everybody", probe.ref)
      probe.expectMessage(HelloWorldCaller.Message("Hello everybody"))
    }
  }
}
