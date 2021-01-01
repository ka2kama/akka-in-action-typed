package aia.config

import akka.actor.ActorSystem
import akka.actor.testkit.typed.scaladsl.ScalaTestWithActorTestKit
import com.typesafe.config.ConfigFactory
import org.scalatest.wordspec.AnyWordSpecLike

class ConfigTest extends ScalaTestWithActorTestKit with AnyWordSpecLike {

  "Configuration" should {
    "has configuration" in {
      val mySystem = ActorSystem("myTest")
      val config   = mySystem.settings.config
      config.getInt("myTest.intParam") should be(20)
      config.getString("myTest.applicationDesc") should be("My Config Test")
    }
    "has defaults" in {
      val mySystem = ActorSystem("myDefaultsTest")
      val config   = mySystem.settings.config
      config.getInt("myTestDefaults.intParam") should be(20)
      config.getString("myTestDefaults.applicationDesc") should be("My Current Test")
    }
    "can include file" in {
      val mySystem = ActorSystem("myIncludeTest")
      val config   = mySystem.settings.config
      config.getInt("myTestIncluded.intParam") should be(20)
      config.getString("myTestIncluded.applicationDesc") should be("My Include Test")
    }
    "can be loaded by ourself" in {
      val configuration = ConfigFactory.load("load")
      val mySystem      = ActorSystem("myLoadTest", configuration)
      val config        = mySystem.settings.config
      config.getInt("myTestLoad.intParam") should be(20)
      config.getString("myTestLoad.applicationDesc") should be("My Load Test")
    }
    /*    "can be lifted" in {
      val configuration = ConfigFactory.load("lift")
      val mySystem = ActorSystem("myFirstLiftTest", configuration.getConfig("myTestLift").withFallback(configuration))
      val config = mySystem.settings.config
      config.getInt("myTest.intParam") must be(20)
      config.getString("myTest.applicationDesc") must be("My Lift Test")
      config.getString("rootParam") must be("root")
      config.getString("myTestLift.rootParam") must be("root")

      //TODO: doesn't work anymore
      // after try to update to akka 2.1 and after restoring withOnlyPath wasn't found anymore
      val mySystem2 = ActorSystem("mySecondLiftTest", configuration.getConfig("myTestLift").withOnlyPath("myTest").withFallback(configuration))
      val config2 = mySystem2.settings.config
      config2.getInt("myTest.intParam") must be(20)
      config2.getString("myTest.applicationDesc") must be("My Lift Test")
      evaluating { config2.getString("rootParam") } must produce[ConfigException.Missing]
      config.getString("myTestLift.rootParam") must be("root")
    }
     */
  }

}
