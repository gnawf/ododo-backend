plugins {
  application
  kotlin("jvm") version "1.2.71"
  id("com.github.johnrengelman.shadow") version "4.0.0"
}

repositories {
  jcenter()
}

application {
  mainClassName = "softeng306.project2.MainKt"
}

dependencies {
  compile(group = "org.jetbrains.kotlin", name = "kotlin-stdlib")
  compile(group = "com.google.code.gson", name = "gson", version = "2.8.5")
  compile(group = "org.mongodb", name = "mongodb-driver-sync", version = "3.8.2")
  compile(group = "com.sparkjava", name = "spark-core", version = "2.7.2")
  compile(group = "com.sparkjava", name = "spark-kotlin", version = "1.0.0-alpha")
}

task("stage") {
  dependsOn("clean", "shadowJar")
}
