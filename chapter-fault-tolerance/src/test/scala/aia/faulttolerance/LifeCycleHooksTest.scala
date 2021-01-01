package aia.faulttolerance

import aia.faulttolerance.LifeCycleHooks.{ForceRestart, SampleMessage}
import akka.actor.testkit.typed.scaladsl.ScalaTestWithActorTestKit
import org.scalatest.wordspec.AnyWordSpecLike

class LifeCycleHooksTest extends ScalaTestWithActorTestKit with AnyWordSpecLike {

  "The Child" should {
    "log lifecycle hooks" in {
      val probe        = createTestProbe[String]()
      val testActorRef = spawn(LifeCycleHooks(), "LifeCycleHooks")
      //   watch(testActorRef)
      testActorRef ! ForceRestart
      testActorRef.tell(SampleMessage("sample", probe.ref))

      probe.expectMessage("sample")
      testKit.stop(testActorRef)
      probe.expectTerminated(testActorRef)

    }
  }
}
