package com.goticks

import akka.actor.typed.scaladsl.Behaviors
import akka.actor.typed.{ActorRef, Behavior}

object TicketSeller {

  sealed trait Command
  case class Add(tickets: Seq[Ticket])                            extends Command
  case class Buy(tickets: Int, replyTo: ActorRef[Tickets])        extends Command
  case class GetEvent(replyTo: ActorRef[Option[BoxOffice.Event]]) extends Command
  case class Cancel(replyTo: ActorRef[Option[BoxOffice.Event]])   extends Command

  case class Ticket(id: Int)
  case class Tickets(event: String, entries: Vector[Ticket] = Vector.empty[Ticket])

  def apply(event: String): Behavior[Command] = new TicketSeller(event).receive(Vector.empty)
}

class TicketSeller private (event: String) {
  import TicketSeller._

  def receive(tickets: Vector[Ticket]): Behavior[Command] = {
    Behaviors.receiveMessage {
      case Add(newTickets) => receive(tickets ++ newTickets)

      case Buy(nrOfTickets, replyTo) =>
        val entries = tickets.take(nrOfTickets)
        if (entries.sizeIs >= nrOfTickets) {
          replyTo ! Tickets(event, entries)
          receive(tickets.drop(nrOfTickets))
        } else {
          replyTo ! Tickets(event)
          Behaviors.same
        }

      case GetEvent(replyTo) =>
        replyTo ! Some(BoxOffice.Event(event, tickets.size))
        Behaviors.same

      case Cancel(replyTo) =>
        replyTo ! Some(BoxOffice.Event(event, tickets.size))
        Behaviors.stopped
    }
  }
}
