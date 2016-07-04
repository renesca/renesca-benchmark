name := "renesca-benchmark"

version := "0.1"

scalaVersion := "2.11.8"

libraryDependencies ++= (
  "com.github.renesca" %% "renesca" % "0.3.2-9" ::
  "com.typesafe.play" %% "play-json" % "2.5.4" ::
  Nil
)
