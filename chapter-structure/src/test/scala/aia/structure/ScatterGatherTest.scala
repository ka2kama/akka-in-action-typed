package aia.structure

import akka.actor.testkit.typed.scaladsl.ScalaTestWithActorTestKit
import org.scalatest.wordspec.AnyWordSpecLike

import java.util.Date
import scala.concurrent.duration._

class ScatterGatherTest extends ScalaTestWithActorTestKit with AnyWordSpecLike {

  val duration: FiniteDuration = 2.seconds

  "The ScatterGather" should {
    "scatter the message and gather them again" in {

      val endProbe     = createTestProbe[PhotoCommand]()
      val aggregateRef = spawn(Aggregator(duration, endProbe.ref))
      val speedRef     = spawn(GetSpeed(aggregateRef))
      val timeRef      = spawn(GetTime(aggregateRef))
      val actorRef     = spawn(RecipientList(Seq(speedRef, timeRef)))

      val photoDate  = new Date()
      val photoSpeed = 60
      val msg        = PhotoMessage("id1", ImageProcessing.createPhotoString(photoDate, photoSpeed))

      actorRef ! msg

      val combinedMsg = PhotoMessage(msg.id, msg.photo, Some(photoDate), Some(photoSpeed))

      endProbe.expectMessage(combinedMsg)

    }
  }
}
