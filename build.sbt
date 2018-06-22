
resolvers += Resolver.sonatypeRepo("snapshots")
scalacOptions += "-Ypartial-unification"

lazy val root = Project("extensible-processor", file("."))
  .settings(
    name := "extensible-processor",
    version := "0.1",
    scalaVersion := "2.12.6"
  )
  .settings(
    libraryDependencies ++= Seq(
      "org.json4s" %% "json4s-ast" % "3.6.0-M4",
      "org.scalaz" %% "scalaz-zio" % "0.1-SNAPSHOT"
    )
  )
