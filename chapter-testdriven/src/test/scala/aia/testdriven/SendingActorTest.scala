package aia.testdriven

import akka.actor.testkit.typed.scaladsl.ScalaTestWithActorTestKit
import akka.actor.typed.scaladsl.Behaviors
import akka.actor.typed.{ActorRef, Behavior}
import org.scalatest.wordspec.AnyWordSpecLike

import scala.util.Random

class SendingActorTest extends ScalaTestWithActorTestKit with AnyWordSpecLike {

  "A Sending Actor" should {
    "send a message to another actor when it has finished processing" in {
      import SendingActor._
      val probe        = createTestProbe[SortedEvents]()
      val behavior     = SendingActor(probe.ref)
      val sendingActor = spawn(behavior, "sendingActor")

      val size         = 1000
      val maxInclusive = 100000

      def randomEvents() =
        (0 until size).map { _ =>
          Event(Random.nextInt(maxInclusive))
        }.toVector

      val unsorted   = randomEvents()
      val sortEvents = SortEvents(unsorted)
      sendingActor ! sortEvents

      val SortedEvents(events) = probe.receiveMessage()
      events.size should be(size)
      unsorted.sortBy(_.id) should be(events)

    }
  }
}

object SendingActor {
  def apply(receiver: ActorRef[SortedEvents]): Behavior[SortEvents] =
    Behaviors.receiveMessage { case SortEvents(unsorted) =>
      receiver ! SortedEvents(unsorted.sortBy(_.id))
      Behaviors.same
    }

  case class Event(id: Long)
  case class SortEvents(unsorted: Vector[Event])
  case class SortedEvents(sorted: Vector[Event])
}
