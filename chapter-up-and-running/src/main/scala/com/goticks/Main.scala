package com.goticks

import akka.NotUsed
import akka.actor.typed.scaladsl.Behaviors
import akka.actor.typed.{ActorSystem, Behavior}
import akka.http.scaladsl.Http
import akka.http.scaladsl.Http.ServerBinding
import akka.util.Timeout
import com.typesafe.config.{Config, ConfigFactory}

import scala.concurrent.{ExecutionContextExecutor, Future}
import scala.util.{Failure, Success}

object Main extends App with RequestTimeout {

  val config = ConfigFactory.load()
  val host   = config.getString("http.host") // 設定からホスト名とポートを取得
  val port   = config.getInt("http.port")

  val rootBehavior: Behavior[NotUsed] = Behaviors.setup { context =>
    implicit val system: ActorSystem[_] = context.system
    implicit val ec: ExecutionContextExecutor =
      system.executionContext // bindAndHandleは暗黙のExecutionContextが必要

    val api = new RestApi(context, requestTimeout(config)).routes // the RestApi provides a Route

    val bindingFuture: Future[ServerBinding] =
      Http().newServerAt(host, port).bind(api) // HTTPサーバーの起動

    bindingFuture
      .map { serverBinding =>
        system.log.info(s"RestApi bound to ${serverBinding.localAddress} ")
      }
      .onComplete {
        case Success(_) =>
          system.log.info(s"Success to bind to $host:$port")
        case Failure(ex) =>
          system.log.error(s"Failed to bind to $host:$port!", ex)
          system.terminate()
      }

    Behaviors.empty
  }

  ActorSystem(rootBehavior, "go-ticks")
}

trait RequestTimeout {
  import scala.concurrent.duration._
  def requestTimeout(config: Config): Timeout = {
    val t = config.getString("akka.http.server.request-timeout")
    val d = Duration(t)
    FiniteDuration(d.length, d.unit)
  }
}
