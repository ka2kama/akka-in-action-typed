package aia.testdriven

import akka.actor.testkit.typed.scaladsl.ScalaTestWithActorTestKit
import akka.actor.typed.Behavior
import akka.actor.typed.scaladsl.Behaviors
import org.scalatest.wordspec.AnyWordSpecLike

package silentactor02 {

  import akka.actor.typed.ActorRef

  class SilentActorTest extends ScalaTestWithActorTestKit with AnyWordSpecLike {

    "A Silent Actor" should {

      "change internal state when it receives a message, single" in {
        import SilentActor._

        val silentActor = createTestProbe[SilentActor.Command]()
        silentActor.ref ! SilentMessage("whisper")
        // silentActor.underlyingActor.state should contain("whisper")
      }

    }
  }

  object SilentActor {
    sealed trait Command
    case class SilentMessage(data: String)                  extends Command
    case class GetState(receiver: ActorRef[Vector[String]]) extends Command

    def apply(): Behavior[Command] = Behaviors.setup { _ =>
      def next(internalState: Vector[String]): Behavior[Command] = Behaviors.receiveMessagePartial {
        case SilentMessage(data) =>
          next(internalState :+ data)
      }

      next(Vector.empty)
    }
  }
}

package silentactor03 {

  import akka.actor.typed.ActorRef

  class SilentActorTest extends ScalaTestWithActorTestKit with AnyWordSpecLike {

    "A Silent Actor" should {

      "change internal state when it receives a message, multi" in {
        import SilentActor._

        val probe       = createTestProbe[Vector[String]]()
        val silentActor = spawn(SilentActor(), "s3")
        silentActor ! SilentMessage("whisper1")
        silentActor ! SilentMessage("whisper2")
        silentActor ! GetState(probe.ref)
        probe.expectMessage(Vector("whisper1", "whisper2"))
      }

    }

  }

  object SilentActor {
    sealed trait Command
    case class SilentMessage(data: String)                  extends Command
    case class GetState(receiver: ActorRef[Vector[String]]) extends Command

    def apply(): Behavior[Command] = Behaviors.setup { _ =>
      def next(internalState: Vector[String]): Behavior[Command] = Behaviors.receiveMessage {
        case SilentMessage(data) =>
          next(internalState :+ data)
        case GetState(receiver) =>
          receiver ! internalState
          Behaviors.same
      }

      next(Vector.empty)
    }
  }
}
