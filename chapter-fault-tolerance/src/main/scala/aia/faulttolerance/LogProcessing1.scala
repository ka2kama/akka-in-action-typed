package aia.faulttolerance

import akka.NotUsed
import akka.actor.typed._
import akka.actor.typed.scaladsl.{ActorContext, Behaviors}

import java.io.File
import java.util.UUID

package dbstrategy1 {

  object LogProcessingApp extends App {
    val sources     = Vector("file:///source1/", "file:///source2/")
    val databaseUrl = "http://mydatabase1"

    val rootBehavior: Behavior[NotUsed] = Behaviors.setup { context =>
      context.spawn(
        LogProcessingSupervisor(sources, databaseUrl),
        LogProcessingSupervisor.name,
      )

      Behaviors.empty
    }

    ActorSystem(rootBehavior, "logprocessing")
  }

  object LogProcessingSupervisor {
    def name = "file-watcher-supervisor"

    def apply(sources: Vector[String], databaseUrl: String): Behavior[NotUsed] = {
      val behavior = Behaviors.setup[NotUsed] { context =>
        val fileWatchers: Vector[ActorRef[_]] = sources.map { source =>
          val dbWriter = context.spawn(DbWriter(databaseUrl), DbWriter.name(databaseUrl))

          val logProcessor = context.spawn(LogProcessor(dbWriter), LogProcessor.name)

          val fileWatcher = context.spawn(FileWatcher(source, logProcessor), FileWatcher.name)
          context.watch(fileWatcher)
          fileWatcher
        }

        new LogProcessingSupervisor(context).receive(fileWatchers)
      }

      val resume = Behaviors
        .supervise(behavior)
        .onFailure[CorruptedFileException](SupervisorStrategy.resume)

      val restart = Behaviors
        .supervise(resume)
        .onFailure[DbBrokenConnectionException](SupervisorStrategy.restart)

      val stop = Behaviors
        .supervise(restart)
        .onFailure[DiskError](SupervisorStrategy.stop)

      stop
    }
  }

  class LogProcessingSupervisor private (context: ActorContext[NotUsed]) {

    def receive(fileWatchers: Vector[ActorRef[_]]): Behavior[NotUsed] = Behaviors.receiveSignal {
      case (_, Terminated(actorRef)) =>
        if (fileWatchers.contains(actorRef)) {
          val removed = fileWatchers.filterNot(_ == actorRef)
          if (removed.isEmpty) {
            context.log.info("Shutting down, all file watchers have failed.")
            context.system.terminate()
            Behaviors.stopped
          } else
            receive(removed)
        } else
          Behaviors.same
    }
  }

  object FileWatcher extends FileWatchingAbilities {
    def name = s"file-watcher-${UUID.randomUUID.toString}"

    sealed trait Command
    case class NewFile(file: File, timeAdded: Long) extends Command
    case class SourceAbandoned(uri: String)         extends Command

    def apply(source: String, logProcessor: ActorRef[LogProcessor.LogFile]): Behavior[Command] =
      Behaviors.setup { context =>
        register(source)

        Behaviors.receiveMessage {
          case NewFile(file, _) =>
            logProcessor ! LogProcessor.LogFile(file)
            Behaviors.same
          case SourceAbandoned(uri) =>
            if (uri == source) Behaviors.stopped
            else Behaviors.same
        }
      }
  }

  object LogProcessor extends LogParsing {
    def name = s"log_processor_${UUID.randomUUID.toString}"
    // 新しいログファイル
    case class LogFile(file: File)

    def apply(dbWriter: ActorRef[DbWriter.Line]): Behavior[LogFile] =
      Behaviors.setup { context =>
        Behaviors.receiveMessage { case LogFile(file) =>
          val lines: Vector[DbWriter.Line] = parse(file)
          lines.foreach(dbWriter ! _)
          Behaviors.same
        }
      }
  }

  object DbWriter {

    def name(databaseUrl: String) =
      s"""db-writer-${databaseUrl.split("/").last}"""

    // LogProcessorアクターによって解析されるログファイルの行
    case class Line(time: Long, message: String, messageType: String)

    def apply(databaseUrl: String): Behavior[Line] = Behaviors.setup { _ =>
      val connection = new DbCon(databaseUrl)

      Behaviors
        .receiveMessage[Line] { case Line(time, message, messageType) =>
          connection.write(
            Map(
              Symbol("time")        -> time,
              Symbol("message")     -> message,
              Symbol("messageType") -> messageType,
            )
          )

          Behaviors.same
        }
        .receiveSignal {
          case (_, signal) if signal == PreRestart || signal == PostStop =>
            connection.close()
            Behaviors.same
        }
    }

  }

  class DbCon(url: String) {

    /** Writes a map to a database.
      * @param map the map to write to the database.
      * @throws DbBrokenConnectionException when the connection is broken. It might be back later
      * @throws DbNodeDownException when the database Node has been removed from the database cluster. It will never work again.
      */
    def write(map: Map[Symbol, Any]): Unit = {
      //
    }

    def close(): Unit = {
      //
    }
  }

  @SerialVersionUID(1L)
  class DiskError(msg: String) extends Error(msg) with Serializable

  @SerialVersionUID(1L)
  class CorruptedFileException(msg: String, val file: File) extends Exception(msg) with Serializable

  @SerialVersionUID(1L)
  class DbBrokenConnectionException(msg: String) extends Exception(msg) with Serializable

  @SerialVersionUID(1L)
  class DbNodeDownException(msg: String) extends Exception(msg) with Serializable

  trait LogParsing {
    import DbWriter._
    // ログファイルの解析。ログファイル内の行から行オブジェクトを作成する
    // ファイルが破損している場合、CorruptedFileExceptionをスローする
    def parse(file: File): Vector[Line] = {
      // ここにパーサーを実装、今はダミー値を返す
      Vector.empty[Line]
    }
  }

  trait FileWatchingAbilities {
    def register(uri: String): Unit = {}
  }
}
