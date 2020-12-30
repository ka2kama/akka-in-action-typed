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
      new BoxOffice(context).init()
    }

}

class BoxOffice private (context: ActorContext[BoxOffice.Command])(implicit timeout: Timeout) {
  import BoxOffice._

  implicit val system: ActorSystem[_] = context.system
  implicit val ec: ExecutionContext   = system.executionContext

  def init(): Behavior[Command] = next(Map.empty)

  def next(eventNameToSeller: Map[String, ActorRef[TicketSeller.Command]]): Behavior[Command] =
    Behaviors.receiveMessage {
      case CreateEvent(name, tickets, replyTo) =>
        def create(): Behavior[Command] = {
          val eventTickets = context.spawn(TicketSeller(name), name)
          val newTickets   = (1 to tickets).map(TicketSeller.Ticket.apply)
          eventTickets ! TicketSeller.Add(newTickets)
          replyTo ! EventCreated(Event(name, tickets))
          next(eventNameToSeller + (name -> eventTickets))
        }
        def eventExists(): Behavior[Command] = {
          replyTo ! EventExists
          Behaviors.same
        }
        eventNameToSeller.get(name).fold(create())(_ => eventExists())

      case GetTickets(event, tickets, replyTo) =>
        def notFound(): Behavior[Command] = {
          replyTo ! TicketSeller.Tickets(event)
          Behaviors.same
        }
        def buy(child: ActorRef[TicketSeller.Command]): Behavior[Command] = {
          child ! TicketSeller.Buy(tickets, replyTo)
          Behaviors.same
        }
        eventNameToSeller.get(event).fold(notFound())(buy)

      case GetEvent(event, replyTo) =>
        def notFound(): Behavior[Command] = {
          replyTo ! None
          Behaviors.same
        }
        def getEvent(child: ActorRef[TicketSeller.Command]): Behavior[Command] = {
          child ! TicketSeller.GetEvent(replyTo)
          Behaviors.same
        }
        eventNameToSeller.get(event).fold(notFound())(getEvent)

      case GetEvents(replyTo) =>
        def getEvents: Iterable[Future[Option[Event]]] =
          eventNameToSeller.keys.map { event =>
            context.self.ask[Option[Event]](GetEvent(event, _))
          }
        def convertToEvents(f: Future[Iterable[Option[Event]]]): Future[Events] = {
          val aa = f.map(_.flatten)
          val bb = aa.map(l => Events(l.toVector))
          bb
        }
        convertToEvents(Future.sequence(getEvents)).foreach { replyTo ! _ }
        Behaviors.same

      case CancelEvent(event, replyTo) =>
        def notFound(): Behavior[Command] = {
          replyTo ! None
          Behaviors.same
        }
        def cancelEvent(seller: ActorRef[TicketSeller.Command]): Behavior[Command] = {
          seller ! TicketSeller.Cancel(replyTo)
          next(eventNameToSeller - event)
        }
        eventNameToSeller.get(event).fold(notFound())(cancelEvent)
    }
}
