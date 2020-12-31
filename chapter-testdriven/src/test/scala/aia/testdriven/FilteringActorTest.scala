package aia.testdriven

import aia.testdriven.FilteringActor.Event
import akka.actor.testkit.typed.scaladsl.ScalaTestWithActorTestKit
import akka.actor.typed.scaladsl.Behaviors
import akka.actor.typed.{ActorRef, Behavior}
import org.scalatest.wordspec.AnyWordSpecLike

class FilteringActorTest extends ScalaTestWithActorTestKit with AnyWordSpecLike {
  "A Filtering Actor" should {

    "filter out particular messages" in {
      import FilteringActor._
      val probe    = createTestProbe[Event]()
      val behavior = FilteringActor(probe.ref, 5)
      val filter   = spawn(behavior, "filter-1")
      filter ! Event(1)
      filter ! Event(2)
      filter ! Event(1)
      filter ! Event(3)
      filter ! Event(1)
      filter ! Event(4)
      filter ! Event(5)
      filter ! Event(5)
      filter ! Event(6)

      // TODO: receiveWhileの代替
//      val eventIds = probe.receiveWhile {
//        case Event(id) if id <= 5 => id
//      }
//      eventIds should be(List(1, 2, 3, 4, 5))
//      probe.expectMessage(Event(6))
    }

    "filter out particular messages using expectNoMsg" in {
      import FilteringActor._
      val probe    = createTestProbe[Event]()
      val behavior = FilteringActor(probe.ref, 5)
      val filter   = spawn(behavior, "filter-2")
      filter ! Event(1)
      filter ! Event(2)
      probe.expectMessage(Event(1))
      probe.expectMessage(Event(2))
      filter ! Event(1)
      probe.expectNoMessage()
      filter ! Event(3)
      probe.expectMessage(Event(3))
      filter ! Event(1)
      probe.expectNoMessage()
      filter ! Event(4)
      filter ! Event(5)
      filter ! Event(5)
      probe.expectMessage(Event(4))
      probe.expectMessage(Event(5))
      probe.expectNoMessage()
    }

  }
}

object FilteringActor {

  def apply(nextActor: ActorRef[Event], bufferSize: Int): Behavior[Event] =
    Behaviors.setup { _ =>
      new FilteringActor(nextActor, bufferSize).init()
    }

  case class Event(id: Long)
}

class FilteringActor private (nextActor: ActorRef[Event], bufferSize: Int) {
  import FilteringActor._

  def init(): Behavior[Event] = next(Vector.empty)

  def next(lastMessages: Vector[Event]): Behavior[Event] = Behaviors.receiveMessage { msg =>
    if (!lastMessages.contains(msg)) {
      val newMessages = lastMessages :+ msg
      nextActor ! msg
      if (newMessages.size > bufferSize) {
        // 最も古いものを破棄
        next(newMessages.tail)
      } else
        next(newMessages)
    } else
      Behaviors.same
  }
}
