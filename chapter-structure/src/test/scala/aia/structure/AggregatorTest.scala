package aia.structure

import akka.actor.testkit.typed.scaladsl.ScalaTestWithActorTestKit
import org.scalatest.wordspec.AnyWordSpecLike

import java.util.Date
import scala.concurrent.duration._

class AggregatorTest extends ScalaTestWithActorTestKit with AnyWordSpecLike {
  val duration: FiniteDuration = 2.seconds

  "The Aggregator" should {
    "aggregate two messages" in {

      val endProbe = createTestProbe[PhotoCommand]()
      val actorRef = spawn(Aggregator(duration, endProbe.ref))
      val photoStr = ImageProcessing.createPhotoString(new Date(), 60)
      val msg1     = PhotoMessage("id1", photoStr, Some(new Date()), None)
      actorRef ! msg1

      val msg2 = PhotoMessage("id1", photoStr, None, Some(60))
      actorRef ! msg2

      val combinedMsg =
        PhotoMessage("id1", photoStr, msg1.creationTime, msg2.speed)

      endProbe.expectMessage(combinedMsg)

    }
    "send message after timeout" in {

      val endProbe = createTestProbe[PhotoCommand]()
      val actorRef = spawn(Aggregator(duration, endProbe.ref))
      val photoStr = ImageProcessing.createPhotoString(new Date(), 60)
      val msg1     = PhotoMessage("id1", photoStr, Some(new Date()), None)
      actorRef ! msg1

      endProbe.expectMessage(msg1)

    }
    "aggregate two messages when restarting" in {

      val endProbe = createTestProbe[PhotoCommand]()
      val actorRef = spawn(Aggregator(duration, endProbe.ref))
      val photoStr = ImageProcessing.createPhotoString(new Date(), 60)

      val msg1 = PhotoMessage("id1", photoStr, Some(new Date()), None)
      actorRef ! msg1

      actorRef ! Exception(new IllegalStateException("restart"))

      val msg2 = PhotoMessage("id1", photoStr, None, Some(60))
      actorRef ! msg2

      val combinedMsg =
        PhotoMessage("id1", photoStr, msg1.creationTime, msg2.speed)

      endProbe.expectMessage(combinedMsg)

    }
  }
}
