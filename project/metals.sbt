// format: off
// DO NOT EDIT! This file is auto-generated.

// This plugin enables semantic information to be produced by sbt.
// It also adds support for debugging using the Debug Adapter Protocol
resolvers += "Sonatype OSS Snapshots" at "https://central.sonatype.com/repository/maven-snapshots"
addSbtPlugin("org.scalameta" % "sbt-metals" % "1.6.1+6-b1f4d21d-SNAPSHOT")

// This plugin adds the BSP debug capability to sbt server.

addSbtPlugin("ch.epfl.scala" % "sbt-debug-adapter" % "4.2.7")

// format: on
