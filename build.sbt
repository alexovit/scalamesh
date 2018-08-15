name := "ScalaMesh"

lazy val buildSettings = Defaults.coreDefaultSettings ++ Seq(
  organization := "it.alexov",
  version := "0.1",
  scalaVersion := Dependencies.scalaLastVersion,
  target := baseDirectory.value / "build",
  scalacOptions ++= List("-unchecked", "-deprecation", "-encoding", "UTF8"),
  resolvers += Resolver.sonatypeRepo("releases")
)

lazy val root = (project in file("."))
  .settings(buildSettings)
  .settings(
    name := "scalamesh",
    libraryDependencies ++= Dependencies.common ++ Dependencies.io ++ Dependencies.tests
  )

