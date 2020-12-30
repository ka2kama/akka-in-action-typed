package aia.structure

import akka.actor.typed.scaladsl.Behaviors
import akka.actor.typed.{ActorRef, Behavior}

case class Photo(license: String, speed: Int)

object SpeedFilter {
  def apply(minSpeed: Int, pipe: ActorRef[Photo]): Behavior[Photo] =
    Behaviors.setup { _ =>
      new SpeedFilter(minSpeed, pipe).init()
    }
}

class SpeedFilter private (minSpeed: Int, pipe: ActorRef[Photo]) {
  def init(): Behavior[Photo] = Behaviors.receiveMessage { msg =>
    if (msg.speed > minSpeed) {
      pipe ! msg
    }
    Behaviors.same
  }
}

object LicenseFilter {
  def apply(pipe: ActorRef[Photo]): Behavior[Photo] =
    Behaviors.setup { _ =>
      new LicenseFilter(pipe).init()
    }
}

class LicenseFilter private (pipe: ActorRef[Photo]) {
  def init(): Behavior[Photo] = Behaviors.receiveMessage { msg =>
    if (msg.license.nonEmpty) {
      pipe ! msg
    }
    Behaviors.same
  }
}
