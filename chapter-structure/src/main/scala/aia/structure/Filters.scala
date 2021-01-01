package aia.structure

import akka.actor.typed.scaladsl.Behaviors
import akka.actor.typed.{ActorRef, Behavior}

case class Photo(license: String, speed: Int)

object SpeedFilter {
  def apply(minSpeed: Int, pipe: ActorRef[Photo]): Behavior[Photo] =
    Behaviors.setup { _ =>
      new SpeedFilter(minSpeed, pipe).receive()
    }
}

class SpeedFilter private (minSpeed: Int, pipe: ActorRef[Photo]) {
  def receive(): Behavior[Photo] = Behaviors.receiveMessage { msg =>
    if (msg.speed > minSpeed) {
      pipe ! msg
    }
    Behaviors.same
  }
}

object LicenseFilter {
  def apply(pipe: ActorRef[Photo]): Behavior[Photo] =
    Behaviors.setup { _ =>
      new LicenseFilter(pipe).receive()
    }
}

private class LicenseFilter(pipe: ActorRef[Photo]) {
  def receive(): Behavior[Photo] = Behaviors.receiveMessage { msg =>
    if (msg.license.nonEmpty) {
      pipe ! msg
    }
    Behaviors.same
  }
}
