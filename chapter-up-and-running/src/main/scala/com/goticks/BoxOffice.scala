package com.goticks

import akka.actor.typed.scaladsl.AskPattern.{Askable, schedulerFromActorSystem}
import akka.actor.typed.scaladsl.{ActorContext, Behaviors}
import akka.actor.typed.{ActorRef, ActorSystem, Behavior}
import akka.util.Timeout

import scala.concurrent.{ExecutionContext, Future}

object BoxOffice {
  def name = "boxOffice"

  sealed trait Command
  case class CreateEvent(name: String, tickets: Int, replyTo: ActorRef[EventResponse])
      extends Command
  case class GetEvent(name: String, replyTo: ActorRef[Option[Event]]) extends Command
  case class GetEvents(replyTo: ActorRef[Events])                     extends Command
  case class GetTickets(event: String, tickets: Int, replyTo: ActorRef[TicketSeller.Tickets])
      extends Command
  case class CancelEvent(name: String, replyTo: ActorRef[Option[Event]]) extends Command

  case class Event(name: String, tickets: Int)
  case class Events(events: Vector[Event])

  sealed trait EventResponse
  case class EventCreated(event: Event) extends EventResponse
  case object EventExists               extends EventResponse

  def apply()(implicit timeout: Timeout): Behavior[Command] =
    Behaviors.setup { context =>
      new BoxOffice(context).updated(Map.empty)
    }

}

class BoxOffice private (context: ActorContext[BoxOffice.Command])(implicit timeout: Timeout) {
  import BoxOffice._

  implicit val system: ActorSystem[_] = context.system
  implicit val ec: ExecutionContext   = system.executionContext

  def updated(eventNameToActor: Map[String, ActorRef[TicketSeller.Command]]): Behavior[Command] =
    Behaviors.receiveMessage {
      case CreateEvent(name, tickets, replyTo) =>
        eventNameToActor.get(name) match {
          case Some(_) =>
            replyTo ! EventExists
            Behaviors.same
          case None =>
            val eventTickets = context.spawn(TicketSeller(name), name)
            val newTickets   = (1 to tickets).map(TicketSeller.Ticket.apply).toVector
            eventTickets ! TicketSeller.Add(newTickets)
            replyTo ! EventCreated(Event(name, tickets))
            updated(eventNameToActor + (name -> eventTickets))
        }

      case GetTickets(event, tickets, replyTo) =>
        def notFound(): Unit = replyTo ! TicketSeller.Tickets(event)
        def buy(child: ActorRef[TicketSeller.Command]): Unit =
          child ! TicketSeller.Buy(tickets, replyTo)

        eventNameToActor.get(event).fold(notFound())(buy)
        Behaviors.same

      case GetEvent(event, replyTo) =>
        def notFound(): Unit = replyTo ! None
        def getEvent(child: ActorRef[TicketSeller.Command]): Unit = {
          child ! TicketSeller.GetEvent(replyTo)
        }

        eventNameToActor.get(event).fold(notFound())(getEvent)
        Behaviors.same

      case GetEvents(replyTo) =>
        def getEvents: Iterable[Future[Option[Event]]] =
          eventNameToActor.keys.map { event =>
            context.self.ask[Option[Event]](GetEvent(event, _))
          }
        def convertToEvents(
            f: Future[Iterable[Option[Event]]]
        ): Future[Events] = {
          val aa = f.map(_.flatten)
          val bb = aa.map(l => Events(l.toVector))
          bb
        }
        convertToEvents(Future.sequence(getEvents)).foreach { replyTo ! _ }
        Behaviors.same

      case CancelEvent(event, replyTo) =>
        eventNameToActor.get(event) match {
          case Some(seller) =>
            seller ! TicketSeller.Cancel(replyTo)
            updated(eventNameToActor - event)
          case None =>
            replyTo ! None
            Behaviors.same
        }
    }
}
