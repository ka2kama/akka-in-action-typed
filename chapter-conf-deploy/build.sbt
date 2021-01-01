name := "deploy"

version := "1.0"

organization := "manning"

scalacOptions ++= Seq(
  "-deprecation",
  "-unchecked",
  "-Xlint",
  "-Ywarn-unused",
  "-Ywarn-dead-code",
  "-feature",
  "-language:_"
)

enablePlugins(JavaAppPackaging)

scriptClasspath +="../conf"

libraryDependencies ++= {
  val akkaVersion     = "2.6.10"
  Seq(
    "com.typesafe.akka" %% "akka-actor-typed"         % akkaVersion,
    "com.typesafe.akka" %% "akka-slf4j"               % akkaVersion,
    "ch.qos.logback"     % "logback-classic"          % "1.2.3",
    "com.typesafe.akka" %% "akka-actor-testkit-typed" % akkaVersion % Test,
    "org.scalatest"     %% "scalatest"                % "3.2.2"     % Test,
  )
}
