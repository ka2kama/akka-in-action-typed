package aia.routing

import akka.actor.testkit.typed.scaladsl.ScalaTestWithActorTestKit
import org.scalatest.wordspec.AnyWordSpecLike

class RouteSlipTest extends ScalaTestWithActorTestKit with AnyWordSpecLike {

  "The Router" should {
    "route messages correctly" in {

      val probe  = createTestProbe[Car]()
      val router = spawn(SlipRouter(probe.ref), "SlipRouter")

      val minimalOrder = Order(Seq())
      router ! minimalOrder
      val defaultCar = Car(color = "black", hasNavigation = false, hasParkingSensors = false)
      probe.expectMessage(defaultCar)

      val fullOrder =
        Order(Seq(CarOptions.CAR_COLOR_GRAY, CarOptions.NAVIGATION, CarOptions.PARKING_SENSORS))
      router ! fullOrder
      val carWithAllOptions =
        Car(color = "gray", hasNavigation = true, hasParkingSensors = true)
      probe.expectMessage(carWithAllOptions)

      val msg = Order(Seq(CarOptions.PARKING_SENSORS))
      router ! msg
      val expectedCar = Car(color = "black", hasNavigation = false, hasParkingSensors = true)
      probe.expectMessage(expectedCar)
    }
  }
}
