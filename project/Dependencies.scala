import sbt._

object Dependencies {

  // format: OFF
  val scalaLastVersion      = "2.12.6"

  val caseAppVersion        = "2.0.0-M3"

  val akkaVersion           = "2.5.11"

  val scalaTestVersion      = "3.0.5"

  object Compile {
    // Common
    val caseApp              = "com.github.alexarchambault"  %% "case-app"     % caseAppVersion

    // IO
    val akkaActor            = "com.typesafe.akka"   %% "akka-actor"           % akkaVersion
    val akkaStream           = "com.typesafe.akka"   %% "akka-stream"          % akkaVersion
  }

  object Test {
    val scalaTest            = "org.scalatest"       %% "scalatest"           % scalaTestVersion  % "test"
  }

  import Compile._
  import Test._

  val common   = Seq(caseApp)
  val io       = Seq(akkaActor, akkaStream)
  val tests    = Seq(scalaTest)

  // format: ON
}