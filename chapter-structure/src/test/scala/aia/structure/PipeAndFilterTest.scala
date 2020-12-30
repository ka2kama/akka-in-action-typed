package aia.structure

import akka.actor.testkit.typed.scaladsl.ScalaTestWithActorTestKit
import org.scalatest.wordspec.AnyWordSpecLike

import scala.concurrent.duration._

class PipeAndFilterTest extends ScalaTestWithActorTestKit with AnyWordSpecLike {

  val duration: FiniteDuration = 2.seconds

  "The pipe and filter" should {
    "filter messages in configuration 1" in {

      val endProbe         = createTestProbe[Photo]()
      val speedFilterRef   = spawn(SpeedFilter(50, endProbe.ref))
      val licenseFilterRef = spawn(LicenseFilter(speedFilterRef))
      val msg              = Photo("123xyz", 60)
      licenseFilterRef ! msg
      endProbe.expectMessage(msg)

      licenseFilterRef ! Photo("", 60)
      endProbe.expectNoMessage(duration)

      licenseFilterRef ! Photo("123xyz", 49)
      endProbe.expectNoMessage(duration)
    }
    "filter messages in configuration 2" in {

      val endProbe         = createTestProbe[Photo]()
      val licenseFilterRef = spawn(LicenseFilter(endProbe.ref))
      val speedFilterRef   = spawn(SpeedFilter(50, licenseFilterRef))
      val msg              = Photo("123xyz", 60)
      speedFilterRef ! msg
      endProbe.expectMessage(msg)

      speedFilterRef ! Photo("", 60)
      endProbe.expectNoMessage(duration)

      speedFilterRef ! Photo("123xyz", 49)
      endProbe.expectNoMessage(duration)
    }
  }
}
