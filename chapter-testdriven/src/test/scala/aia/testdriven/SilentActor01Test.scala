package aia.testdriven

import akka.actor._
import akka.actor.testkit.typed.scaladsl.ScalaTestWithActorTestKit
import org.scalatest.wordspec.AnyWordSpecLike

//This test is ignored in the BookBuild, it's added to the defaultExcludedNames

class SilentActor01Test extends ScalaTestWithActorTestKit with AnyWordSpecLike {
  // travisのビルドをパスするためコメントアウトしますが、こちらが書籍内のテストです。
  // "A Silent Actor" must {
  //   "change state when it receives a message, single threaded" in {
  //     // テストを書くと最初は失敗する
  //     fail("not implemented yet")
  //   }
  //   "change state when it receives a message, multi-threaded" in {
  //     // テストを書くと最初は失敗する
  //     fail("not implemented yet")
  //   }
  // }
  "A Silent Actor" must {
    "change state when it receives a message, single threaded" ignore {
      // テストを書くと最初は失敗する
      fail("not implemented yet")
    }
    "change state when it receives a message, multi-threaded" ignore {
      // テストを書くと最初は失敗する
      fail("not implemented yet")
    }
  }

}

class SilentActor extends Actor {
  def receive: Receive = { case _ =>
  }
}
