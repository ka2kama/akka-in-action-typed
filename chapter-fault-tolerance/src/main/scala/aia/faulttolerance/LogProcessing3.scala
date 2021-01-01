package aia.faulttolerance

import akka.NotUsed
import akka.actor.typed._
import akka.actor.typed.scaladsl.{AbstractBehavior, ActorContext, Behaviors}

import java.io.File
import scala.concurrent.duration._
import scala.language.postfixOps

package dbstrategy3 {

  object LogProcessingApp extends App {
    val sources = Vector("file:///source1/", "file:///source2/")
    // propsと依存関係を生成
    val databaseUrl = "http://mydatabase"

    val writerBehavior       = DbWriter(databaseUrl)
    val dbSuperBehavior      = DbSupervisor(writerBehavior)
    val logProcSuperBehavior = LogProcSupervisor(dbSuperBehavior)
    val topLevelBehavior     = FileWatcherSupervisor(sources, logProcSuperBehavior)

    ActorSystem(topLevelBehavior, "logprocessing")
  }

  object FileWatcherSupervisor {
    import LogProcessingProtocol._

    def apply(sources: Vector[String], logProcSuperProps: Behavior[LogFile]): Behavior[NotUsed] =
      Behaviors
        .supervise {
          Behaviors.setup[NotUsed] { context =>
            var fileWatchers = sources.map { source =>
              val logProcSupervisor = context.spawnAnonymous(logProcSuperProps)
              val fileWatcher       = context.spawnAnonymous(FileWatcher(source, logProcSupervisor))
              context.watch(fileWatcher)
              fileWatcher
            }

            Behaviors.receiveSignal { case (_, Terminated(fileWatcher)) =>
              fileWatchers = fileWatchers.filterNot(w => w == fileWatcher)
              if (fileWatchers.isEmpty)
                Behaviors.stopped
              else
                Behaviors.same
            }
          }
        }
        .onFailure[DiskError](SupervisorStrategy.stop)
  }

  object FileWatcher extends FileWatchingAbilities {
    import FileWatcherProtocol._
    import LogProcessingProtocol._

    def apply(
        sourceUri: String,
        logProcSupervisor: ActorRef[LogFile],
    ): Behavior[FileWatcherProtocol.Command] = {
      register(sourceUri)

      Behaviors.receiveMessage {
        case NewFile(file, _) =>
          logProcSupervisor ! LogFile(file)
          Behaviors.same
        case SourceAbandoned(uri) =>
          if (uri == sourceUri)
            Behaviors.stopped
          else
            Behaviors.same
      }
    }
  }

  object LogProcSupervisor {
    import LogProcessingProtocol._

    def apply(dbSupervisorProps: Behavior[Line]): Behavior[LogFile] = {
      Behaviors
        .supervise {
          Behaviors.setup[LogFile] { context =>
            val dbSupervisor: ActorRef[Line]    = context.spawnAnonymous(dbSupervisorProps)
            val logProcProps: Behavior[LogFile] = LogProcessor(dbSupervisor)
            val logProcessor: ActorRef[LogFile] = context.spawnAnonymous(logProcProps)

            Behaviors.receiveMessage { m =>
              logProcessor ! m
              Behaviors.same
            }
          }
        }
        .onFailure[CorruptedFileException](SupervisorStrategy.resume)
    }
  }

  object LogProcessor extends LogParsing {
    import LogProcessingProtocol._

    def apply(dbSupervisor: ActorRef[Line]): Behavior[LogFile] =
      Behaviors.receiveMessage { case LogFile(file) =>
        val lines = parse(file)
        lines.foreach(dbSupervisor ! _)
        Behaviors.same
      }
  }

  object DbImpatientSupervisor {

    import LogProcessingProtocol._

    def apply(writerBehavior: Behavior[Line]): Behavior[Line] = {
      Behaviors
        .supervise {
          Behaviors.setup[Line] { context =>
            val writer = context.spawnAnonymous(writerBehavior)

            Behaviors.receiveMessage { line =>
              writer ! line
              Behaviors.same
            }
          }
        }
        .onFailure[DbBrokenConnectionException](
          SupervisorStrategy.restart.withLimit(maxNrOfRetries = 5, withinTimeRange = 60.seconds)
        )
    }
  }

  object DbSupervisor {

    import LogProcessingProtocol._

    def apply(writerBehavior: Behavior[Line]): Behavior[Line] = {
      Behaviors
        .supervise {
          Behaviors.setup[Line] { context =>
            val writer = context.spawnAnonymous(writerBehavior)

            Behaviors.receiveMessage { line =>
              writer ! line
              Behaviors.same
            }
          }
        }
        .onFailure[DbBrokenConnectionException](SupervisorStrategy.restart)
    }
  }

  object DbWriter {
    import LogProcessingProtocol._
    def apply(databaseUrl: String): Behavior[Line] =
      Behaviors.setup { _ =>
        val connection = new DbCon(databaseUrl)

        Behaviors.receiveMessage { case Line(time, message, messageType) =>
          connection.write(
            Map(
              Symbol("time")        -> time,
              Symbol("message")     -> message,
              Symbol("messageType") -> messageType,
            )
          )
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

  trait LogParsing {
    import LogProcessingProtocol._
    // ログファイルの解析。ログファイル内の行から行オブジェクトを作成する
    // ファイルが破損している場合、CorruptedFileExceptionをスローする
    def parse(file: File): Vector[Line] = {
      // ここにパーサーを実装、今はダミー値を返す
      Vector.empty[Line]
    }
  }
  object FileWatcherProtocol {
    sealed trait Command
    case class NewFile(file: File, timeAdded: Long) extends Command
    case class SourceAbandoned(uri: String)         extends Command
  }
  trait FileWatchingAbilities {
    def register(uri: String): Unit = {}
  }

  object LogProcessingProtocol {
    // 新しいログファイル
    case class LogFile(file: File)
    // LogProcessorアクターによって解析されるログファイルの行
    case class Line(time: Long, message: String, messageType: String)
  }

}
