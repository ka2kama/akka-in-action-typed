package com.goticks

import akka.actor.typed.scaladsl.ActorContext
import akka.actor.typed.scaladsl.AskPattern.{Askable, schedulerFromActorSystem}
import akka.actor.typed.{ActorRef, ActorSystem}
import akka.http.scaladsl.marshallers.sprayjson.SprayJsonSupport._
import akka.http.scaladsl.model.StatusCodes._
import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.server._
import akka.util.Timeout

import scala.concurrent.{ExecutionContext, Future}

class RestApi(context: ActorContext[_], timeout: Timeout) extends RestRoutes {
  implicit val system: ActorSystem[_]             = context.system
  implicit val executionContext: ExecutionContext = system.executionContext
  implicit val requestTimeout: Timeout            = timeout

  val boxOffice: ActorRef[BoxOffice.Command] = context.spawn(BoxOffice(), BoxOffice.name)
}

trait RestRoutes extends BoxOfficeApi with EventMarshalling {

  def routes: Route = concat(eventsRoute, eventRoute, ticketsRoute)

  def eventsRoute: Route =
    pathPrefix("events") {
      pathEndOrSingleSlash {
        get {
          // GET /events
          onSuccess(getEvents()) { events =>
            complete(OK, events)
          }
        }
      }
    }

  def eventRoute: Route =
    pathPrefix("events" / Segment) { event =>
      pathEndOrSingleSlash {
        concat(
          post {
            // POST /events/:event
            entity(as[EventDescription]) { ed =>
              onSuccess(createEvent(event, ed.tickets)) {
                case BoxOffice.EventCreated(event) => complete(Created, event)
                case BoxOffice.EventExists =>
                  val err = Error(s"$event event exists already.")
                  complete(BadRequest, err)
              }
            }
          },
          get {
            // GET /events/:event
            onSuccess(getEvent(event)) {
              _.fold(complete(NotFound))(e => complete(OK, e))
            }
          },
          delete {
            // DELETE /events/:event
            onSuccess(cancelEvent(event)) {
              _.fold(complete(NotFound))(e => complete(OK, e))
            }
          },
        )
      }
    }

  def ticketsRoute: Route =
    pathPrefix("events" / Segment / "tickets") { event =>
      post {
        pathEndOrSingleSlash {
          // POST /events/:event/tickets
          entity(as[TicketRequest]) { request =>
            onSuccess(requestTickets(event, request.tickets)) { tickets =>
              if (tickets.entries.isEmpty) complete(NotFound)
              else complete(Created, tickets)
            }
          }
        }
      }
    }

}

trait BoxOfficeApi {
  import BoxOffice._

  implicit def system: ActorSystem[_]
  implicit def executionContext: ExecutionContext
  implicit def requestTimeout: Timeout

  def boxOffice: ActorRef[BoxOffice.Command]

  def createEvent(event: String, nrOfTickets: Int): Future[EventResponse] =
    boxOffice.ask(CreateEvent(event, nrOfTickets, _))

  def getEvents(): Future[Events] =
    boxOffice.ask(GetEvents.apply)

  def getEvent(event: String): Future[Option[Event]] =
    boxOffice.ask(GetEvent(event, _))

  def cancelEvent(event: String): Future[Option[Event]] =
    boxOffice.ask(CancelEvent(event, _))

  def requestTickets(event: String, tickets: Int): Future[TicketSeller.Tickets] =
    boxOffice.ask(GetTickets(event, tickets, _))
}
