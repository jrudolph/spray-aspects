import sbt._
import Keys._

import com.typesafe.sbt.SbtAspectj.{ Aspectj, aspectjSettings, compiledClasses, useInstrumentedClasses }
import com.typesafe.sbt.SbtAspectj.AspectjKeys.{ inputs, binaries }

import sbtassembly.Plugin._
import AssemblyKeys._

object SprayAspectBuild extends Build {
  import Dependencies._

  lazy val root =
    Project("root", file("."))
      .aggregate(sprayAspects, example)

  lazy val sprayAspects =
    Project("spray-aspects", file("spray-aspects"))
      .settings(basicSettings: _*)
      .settings(aspectjSettings: _*)
      .settings(
        libraryDependencies ++= Seq(
          sprayClient % "provided",
          akkaActor % "provided"
        ),
        inputs in Aspectj <+= compiledClasses,
        products in Compile <<= products in Aspectj
      )

  lazy val example =
    Project("example", file("example"))
      .settings(basicSettings: _*)
      .settings(aspectjSettings: _*)
      .settings(assemblySettings: _*)
      .settings(
        libraryDependencies ++= Seq(
          sprayClient,
          sprayJson,
          akkaActor,
          akkaSlf4j,
          logback
        ),
        addReflect,
        fork in run := true,

        // we only need that if we are calling weaved classes, if I understand it correctly
        // inputs in Aspectj <+= compiledClasses,

        inputs in Aspectj <++= (externalDependencyClasspath in Compile).map { cp =>
          val whitelist = Seq("spray-io", "spray-can", "scala-reflect")
          cp.map(_.data).filter(f => whitelist.exists(f.getName.contains))
        },

        /* an alternative way to selecting dependency jars to weave
        inputs in Aspectj <++= update map { report =>
          report.matching(moduleFilter(organization = "io.spray", name = "spray-io*"))
        },*/

        // configure our aspects
        binaries in Aspectj <<= products in Compile in sprayAspects,
        // reconfigure our products to use the output of aspectj
        products in Compile <<= products in Aspectj,

        // use instrumented classes when running
        fullClasspath in Runtime <<= useInstrumentedClasses(Runtime)
      )
      .dependsOn(sprayAspects)

  def basicSettings = seq(
    scalaVersion := "2.10.2",
    resolvers += "spray repo" at "http://repo.spray.io/"
  )

  object Dependencies {
    val akkaV = "2.1.4"
    val sprayV = "1.1-M8"

    val sprayClient = "io.spray" % "spray-client" % sprayV
    val sprayJson = "io.spray" %% "spray-json" % "1.2.4"

    val akkaActor = "com.typesafe.akka" %% "akka-actor" % akkaV
    val akkaSlf4j = "com.typesafe.akka" %% "akka-slf4j" % akkaV % "runtime"
    val logback = "ch.qos.logback" % "logback-classic" % "1.0.7" % "runtime"

    val addReflect = libraryDependencies <+= scalaVersion("org.scala-lang" % "scala-reflect" % _ % "provided")
  }
}
