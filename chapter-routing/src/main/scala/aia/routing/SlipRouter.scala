package aia.routing

import akka.actor.typed.scaladsl.{ActorContext, Behaviors}
import akka.actor.typed.{ActorRef, Behavior}

import scala.annotation.unused
import scala.collection.mutable.ListBuffer

object CarOptions extends Enumeration {
  val CAR_COLOR_GRAY, NAVIGATION, PARKING_SENSORS = Value
}

case class Order(options: Seq[CarOptions.Value])
case class Car(
    color: String = "",
    hasNavigation: Boolean = false,
    hasParkingSensors: Boolean = false,
)

case class RouteSlipTasks(routeSlip: List[ActorRef[RouteSlipMessage]], endStep: ActorRef[Car])
case class RouteSlipMessage(nextTasks: RouteSlipTasks, progress: Car)

trait RouteSlip {

  def sendMessageToNextTask(
      nextTasks: RouteSlipTasks,
      progress: Car,
  ): Unit = nextTasks.routeSlip match {
    case Nil => nextTasks.endStep ! progress
    case nextTask :: newSlip =>
      nextTask ! RouteSlipMessage(nextTasks.copy(routeSlip = newSlip), progress = progress)
  }
}

object PaintCar {
  def apply(color: String): Behavior[RouteSlipMessage] = Behaviors.setup { context =>
    new PaintCar(context, color).receive()
  }
}

class PaintCar private (@unused context: ActorContext[RouteSlipMessage], color: String)
    extends RouteSlip {
  def receive(): Behavior[RouteSlipMessage] = Behaviors.receiveMessage {
    case RouteSlipMessage(nextTasks, car: Car) =>
      sendMessageToNextTask(nextTasks, car.copy(color = color))
      Behaviors.same
  }
}

object AddNavigation {
  def apply(): Behavior[RouteSlipMessage] = Behaviors.setup { context =>
    new AddNavigation(context).receive()
  }
}

class AddNavigation private (@unused context: ActorContext[RouteSlipMessage]) extends RouteSlip {
  def receive(): Behavior[RouteSlipMessage] = Behaviors.receiveMessage {
    case RouteSlipMessage(nextTasks, car: Car) =>
      sendMessageToNextTask(nextTasks, car.copy(hasNavigation = true))
      Behaviors.same
  }
}

object AddParkingSensors {
  def apply(): Behavior[RouteSlipMessage] = Behaviors.setup { context =>
    new AddParkingSensors(context).receive()
  }
}

class AddParkingSensors private (@unused context: ActorContext[RouteSlipMessage])
    extends RouteSlip {
  def receive(): Behavior[RouteSlipMessage] = Behaviors.receiveMessagePartial {
    case RouteSlipMessage(nextTasks, car: Car) =>
      sendMessageToNextTask(nextTasks, car.copy(hasParkingSensors = true))
      Behaviors.same
  }
}

object SlipRouter {
  def apply(endStep: ActorRef[Car]): Behavior[Order] =
    Behaviors.setup { context =>
      new SlipRouter(context, endStep).receive()
    }
}

class SlipRouter(context: ActorContext[Order], endStep: ActorRef[Car]) extends RouteSlip {

  val paintBlack: ActorRef[RouteSlipMessage]    = context.spawn(PaintCar("black"), "paintBlack")
  val paintGray: ActorRef[RouteSlipMessage]     = context.spawn(PaintCar("gray"), "paintGray")
  val addNavigation: ActorRef[RouteSlipMessage] = context.spawn(AddNavigation(), "navigation")
  val addParkingSensor: ActorRef[RouteSlipMessage] =
    context.spawn(AddParkingSensors(), "parkingSensors")

  def receive(): Behavior[Order] = Behaviors.receiveMessage { case Order(options) =>
    val routeSlip = createRouteSlip(options)
    sendMessageToNextTask(RouteSlipTasks(routeSlip, endStep), Car())
    Behaviors.same
  }

  private def createRouteSlip(options: Seq[CarOptions.Value]): List[ActorRef[RouteSlipMessage]] = {

    val routeSlip = new ListBuffer[ActorRef[RouteSlipMessage]]
    //car needs a color
    if (!options.contains(CarOptions.CAR_COLOR_GRAY)) {
      routeSlip += paintBlack
    }
    options.foreach {
      case CarOptions.CAR_COLOR_GRAY  => routeSlip += paintGray
      case CarOptions.NAVIGATION      => routeSlip += addNavigation
      case CarOptions.PARKING_SENSORS => routeSlip += addParkingSensor
      case _                          => //do nothing
    }
    routeSlip.toList
  }
}
