package aia.testdriven

import akka.actor.testkit.typed.scaladsl.ScalaTestWithActorTestKit
import akka.actor.typed.scaladsl.AskPattern.{Askable, schedulerFromActorSystem}
import akka.actor.typed.scaladsl.Behaviors
import akka.actor.typed.{ActorRef, Behavior}
import akka.util.Timeout
import org.scalatest.wordspec.AnyWordSpecLike

import scala.concurrent.duration.DurationInt
import scala.concurrent.{Await, ExecutionContextExecutor}
import scala.util.{Failure, Success}

class EchoActorTest extends ScalaTestWithActorTestKit with AnyWordSpecLike {

  "An EchoActor" should {
    "Reply with the same message it receives" in {

      implicit val timeout: Timeout             = Timeout(3.seconds)
      implicit val ec: ExecutionContextExecutor = system.executionContext

      val echo   = spawn(EchoActor())
      val future = echo.ask(EchoMessage("some message", _))
      future.onComplete {
        case Failure(_) => // 失敗時の制御
        case Success(_) => // 成功時の制御
      }

      Await.ready(future, timeout.duration)
    }

    "Reply with the same message it receives without ask" in {
      val probe = createTestProbe[String]()
      val echo  = spawn(EchoActor())
      echo ! EchoMessage("some message", probe.ref)
      probe.expectMessage("some message")

    }

  }
}

case class EchoMessage(message: String, replyTo: ActorRef[String])
object EchoActor {
  def apply(): Behavior[EchoMessage] = Behaviors.receiveMessage {
    case EchoMessage(message, replyTo) =>
      replyTo ! message
      Behaviors.same
  }
}
