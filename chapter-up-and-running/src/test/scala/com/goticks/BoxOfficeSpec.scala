package com.goticks

import akka.actor.testkit.typed.scaladsl.ScalaTestWithActorTestKit
import com.goticks.BoxOffice._
import com.goticks.TicketSeller._
import org.scalatest.wordspec.AnyWordSpecLike

class BoxOfficeSpec extends ScalaTestWithActorTestKit with AnyWordSpecLike {
  "The BoxOffice" should {

    "Create an event and get tickets from the correct Ticket Seller" in {

      val replyProbe = createTestProbe[BoxOffice.EventResponse]()
      val boxOffice  = spawn(BoxOffice())
      val eventName  = "RHCP"
      boxOffice ! CreateEvent(eventName, 10, replyProbe.ref)
      replyProbe.expectMessage(EventCreated(Event(eventName, 10)))

      val eventsProbe = createTestProbe[Events]()
      boxOffice ! GetEvents(eventsProbe.ref)
      eventsProbe.expectMessage(Events(Vector(Event(eventName, 10))))

      val eventProbe = createTestProbe[Option[Event]]()
      boxOffice ! BoxOffice.GetEvent(eventName, eventProbe.ref)
      eventProbe.expectMessage(Some(Event(eventName, 10)))

      val ticketProbe = createTestProbe[TicketSeller.Tickets]()
      boxOffice ! GetTickets(eventName, 1, ticketProbe.ref)
      ticketProbe.expectMessage(Tickets(eventName, Vector(Ticket(1))))

      boxOffice ! GetTickets("DavidBowie", 1, ticketProbe.ref)
      ticketProbe.expectMessage(Tickets("DavidBowie"))
    }

    "Create a child actor when an event is created and sends it a Tickets message" in {
      val probe     = createTestProbe[BoxOffice.EventResponse]()
      val boxOffice = spawn(BoxOffice())

      val tickets         = 3
      val eventName       = "RHCP"
      val expectedTickets = (1 to tickets).map(Ticket).toVector
      boxOffice ! CreateEvent(eventName, tickets, probe.ref)
      //expectMsg(Add(expectedTickets))
      probe.expectMessage(EventCreated(Event(eventName, tickets)))
    }

    "Get and cancel an event that is not created yet" in {
      val probe     = createTestProbe[Option[Event]]()
      val boxOffice = spawn(BoxOffice())

      val noneExitEventName = "noExitEvent"
      boxOffice ! BoxOffice.GetEvent(noneExitEventName, probe.ref)
      probe.expectMessage(None)

      boxOffice ! CancelEvent(noneExitEventName, probe.ref)
      probe.expectMessage(None)
    }

    "Cancel a ticket which event is not created " in {
      val probe             = createTestProbe[Option[Event]]()
      val boxOffice         = spawn(BoxOffice())
      val noneExitEventName = "noExitEvent"

      boxOffice ! CancelEvent(noneExitEventName, probe.ref)
      probe.expectMessage(None)
    }

    "Cancel a ticket which event is created" in {
      val probe     = createTestProbe[BoxOffice.EventResponse]()
      val boxOffice = spawn(BoxOffice())
      val eventName = "RHCP"
      val tickets   = 10
      boxOffice ! CreateEvent(eventName, tickets, probe.ref)
      probe.expectMessage(EventCreated(Event(eventName, tickets)))

      val cancelProbe = createTestProbe[Option[Event]]()
      boxOffice ! CancelEvent(eventName, cancelProbe.ref)
      cancelProbe.expectMessage(Some(Event(eventName, tickets)))
    }
  }

}
