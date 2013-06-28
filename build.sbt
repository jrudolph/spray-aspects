import com.typesafe.sbt.SbtAspectj.{ Aspectj, aspectjSettings }
import com.typesafe.sbt.SbtAspectj.AspectjKeys.{ inputs }

resolvers += "spray repo" at "http://repo.spray.io/"

libraryDependencies ++= Seq(
  "io.spray" % "spray-client" % "1.1-M8",
  "io.spray" %% "spray-json" % "1.2.4",
  "org.scala-lang" % "scala-reflect" % "2.10.2",
  "com.typesafe.akka" %% "akka-actor" % "2.1.4",
  "com.typesafe.akka" %% "akka-slf4j" % "2.1.4",
  "ch.qos.logback" % "logback-classic" % "1.0.7"
)

scalaVersion := "2.10.2"

aspectjSettings

inputs in Aspectj <+= compiledClasses

inputs in Aspectj <++= (externalDependencyClasspath in Compile).map { cp =>
  val whitelist = Seq("spray-io", "scala-reflect")
  cp.map(_.data).filter(f => whitelist.exists(f.getName.contains))
}

// use the results of aspectj weaving
products in Compile <<= products in Aspectj

products in Runtime <<= products in Compile

fork in run := true
