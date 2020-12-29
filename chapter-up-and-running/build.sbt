name := "chapter-up-and-running"

version := "0.1"

scalaVersion := "2.13.4"

val akkaVersion = "2.6.10"
libraryDependencies += "com.typesafe.akka" %% "akka-actor-typed" % akkaVersion

libraryDependencies ++= {
  val akkaHttpVersion = "10.2.2"
  Seq(
    "com.typesafe.akka" %% "akka-actor"           % akkaVersion,
    "com.typesafe.akka" %% "akka-stream"          % akkaVersion,
    "com.typesafe.akka" %% "akka-http-core"       % akkaHttpVersion,
    "com.typesafe.akka" %% "akka-http"            % akkaHttpVersion,
    "com.typesafe.akka" %% "akka-http-spray-json" % akkaHttpVersion,
    "com.typesafe.akka" %% "akka-slf4j"           % akkaVersion,
    "ch.qos.logback"     % "logback-classic"      % "1.2.3",
    "com.typesafe.akka" %% "akka-testkit"         % akkaVersion % "test",
    "org.scalatest"     %% "scalatest"            % "3.2.2"     % "test",
  )
}
