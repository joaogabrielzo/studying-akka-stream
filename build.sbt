name := "akka-streams"

version := "0.1"

scalaVersion := "2.12.8"

lazy val akkaVersion = "2.5.19"
lazy val scalaTestVersion = "3.1.1"

libraryDependencies ++= Seq(
  "com.typesafe.akka" %% "akka-stream" % akkaVersion,
  "com.typesafe.akka" %% "akka-stream-testkit" % akkaVersion,
  "com.typesafe.akka" %% "akka-testkit" % akkaVersion
  )