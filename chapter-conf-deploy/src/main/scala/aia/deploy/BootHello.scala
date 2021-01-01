package aia.deploy

import akka.NotUsed
import akka.actor.typed.{ActorSystem, Behavior}
import akka.actor.typed.scaladsl.Behaviors

import scala.concurrent.duration._

object BootHello extends App {

  val rootBehavior: Behavior[NotUsed] = Behaviors.setup { context =>
    val system = context.system

    val actor  = context.spawnAnonymous(HelloWorld())
    val config = system.settings.config
    val timer  = config.getInt("helloWorld.timer")
    context.spawnAnonymous(HelloWorldCaller(timer.millis, actor))

    Behaviors.empty
  }

  ActorSystem(rootBehavior, "hellokernel")
}
