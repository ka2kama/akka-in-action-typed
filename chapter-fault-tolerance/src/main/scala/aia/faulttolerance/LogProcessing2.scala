package aia.faulttolerance

import akka.NotUsed
import akka.actor.typed._
import akka.actor.typed.scaladsl.{ActorContext, Behaviors}

import java.io.File
import java.util.UUID

package dbstrategy2 {

  import akka.actor.typed.scaladsl.AbstractBehavior

  object LogProcessingApp extends App {
    val sources = Vector("file:///source1/", "file:///source2/")

    val databaseUrls = Vector(
      "http://mydatabase1",
      "http://mydatabase2",
      "http://mydatabase3",
    )

    val rootBehavior: Behavior[NotUsed] = Behaviors.setup { context =>
      context.spawn(
        LogProcessingSupervisor(sources, databaseUrls),
        LogProcessingSupervisor.name,
      )

      Behaviors.empty
    }

    ActorSystem(rootBehavior, "logprocessing")
  }

  object LogProcessingSupervisor {
    def name = "file-watcher-supervisor"

    def apply(sources: Vector[String], databaseUrls: Vector[String]): Behavior[NotUsed] = {
      val behavior = Behaviors.setup[NotUsed] { context =>
        new LogProcessingSupervisor(context).init(sources, databaseUrls)
      }

      Behaviors.supervise(behavior).onFailure[DiskError](SupervisorStrategy.stop)
    }
  }

  class LogProcessingSupervisor private (context: ActorContext[_]) {

    def init(sources: Vector[String], databaseUrls: Vector[String]): Behavior[NotUsed] = {
      val fileWatchers: Vector[ActorRef[FileWatcher.Command]] = sources.map { source =>
        val fileWatcher = context.spawnAnonymous(FileWatcher(source, databaseUrls))
        context.watch(fileWatcher)
        fileWatcher
      }

      receive(fileWatchers)
    }

    def receive(fileWatchers: Vector[ActorRef[FileWatcher.Command]]): Behavior[NotUsed] =
      Behaviors.receiveSignal { case (_, Terminated(fileWatcher)) =>
        val newFileWatchers = fileWatchers.filterNot(_ == fileWatcher)
        if (newFileWatchers.isEmpty) {
          context.log.info("Shutting down, all file watchers have failed.")
          context.system.terminate()
          Behaviors.stopped
        } else
          receive(newFileWatchers)
      }
  }

  object FileWatcher {
    sealed trait Command
    case class NewFile(file: File, timeAdded: Long) extends Command
    case class SourceAbandoned(uri: String)         extends Command

    def apply(source: String, databaseUrls: Vector[String]): Behavior[Command] =
      Behaviors
        .supervise[Command] {
          Behaviors.setup { context =>
            new FileWatcher(context, source, databaseUrls)
          }
        }
        .onFailure[CorruptedFileException](SupervisorStrategy.resume)
  }

  class FileWatcher private (
      context: ActorContext[FileWatcher.Command],
      source: String,
      databaseUrls: Vector[String],
  ) extends AbstractBehavior[FileWatcher.Command](context)
      with FileWatchingAbilities {

    import FileWatcher._

    register(source)

    val logProcessor: ActorRef[LogProcessor.LogFile] = context.spawn(
      LogProcessor(databaseUrls),
      LogProcessor.name,
    )

    context.watch(logProcessor)

    override def onMessage(msg: Command): Behavior[Command] = msg match {
      case NewFile(file, _) =>
        logProcessor ! LogProcessor.LogFile(file)
        this
      case SourceAbandoned(uri) =>
        if (uri == source) {
          context.log.info(s"$uri abandoned, stopping file watcher.")
          Behaviors.stopped
        } else this
    }

    override def onSignal: PartialFunction[Signal, Behavior[Command]] = {
      case Terminated(`logProcessor`) =>
        context.log.info(s"Log processor terminated, stopping file watcher.")
        Behaviors.stopped
    }
  }

  object LogProcessor {
    def name = s"log_processor_${UUID.randomUUID.toString}"
    // 新しいログファイル
    case class LogFile(file: File)

    def apply(databaseUrls: Vector[String]): Behavior[LogFile] = {

      val behavior: Behavior[LogFile] = Behaviors.setup { context =>
        new LogProcessor(context).init(databaseUrls)
      }

      val restart = Behaviors
        .supervise(behavior)
        .onFailure[DbBrokenConnectionException](SupervisorStrategy.restart)

      val stop = Behaviors
        .supervise(restart)
        .onFailure[DbBrokenConnectionException](SupervisorStrategy.stop)

      stop
    }
  }

  class LogProcessor private (context: ActorContext[LogProcessor.LogFile]) extends LogParsing {

    import LogProcessor._

    def init(databaseUrls: Vector[String]): Behavior[LogFile] = {
      require(databaseUrls.nonEmpty)

      val initialDatabaseUrl: String         = databaseUrls.head
      val alternateDatabases: Vector[String] = databaseUrls.tail

      val dbWriter: ActorRef[DbWriter.Line] = context.spawn(
        DbWriter(initialDatabaseUrl),
        DbWriter.name(initialDatabaseUrl),
      )
      context.watch(dbWriter)

      receive(dbWriter, alternateDatabases)
    }

    def receive(
        dbWriter: ActorRef[DbWriter.Line],
        alternateDatabases: Vector[String],
    ): Behavior[LogFile] =
      Behaviors
        .receiveMessage[LogFile] { case LogFile(file) =>
          val lines: Vector[DbWriter.Line] = parse(file)
          lines.foreach(dbWriter ! _)
          Behaviors.same
        }
        .receiveSignal { case (_, Terminated(_)) =>
          if (alternateDatabases.nonEmpty) {
            init(alternateDatabases)
          } else {
            context.log.error("All Db nodes broken, stopping.")
            Behaviors.stopped
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
        .receiveSignal { case (_, PostStop) =>
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
