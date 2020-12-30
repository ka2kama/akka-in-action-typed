package aia.structure

import akka.actor.testkit.typed.scaladsl.ScalaTestWithActorTestKit
import org.scalatest.wordspec.AnyWordSpecLike

class RecipientListTest extends ScalaTestWithActorTestKit with AnyWordSpecLike {

  "The RecipientList" should {
    "scatter the message" in {

      val endProbe1 = createTestProbe[String]()
      val endProbe2 = createTestProbe[String]()
      val endProbe3 = createTestProbe[String]()
      val list      = Seq(endProbe1.ref, endProbe2.ref, endProbe3.ref)
      val actorRef  = spawn(RecipientList(list))
      val msg       = "message"
      actorRef ! msg
      endProbe1.expectMessage(msg)
      endProbe2.expectMessage(msg)
      endProbe3.expectMessage(msg)

    }
  }
}
