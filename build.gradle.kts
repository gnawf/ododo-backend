plugins {
  application
  kotlin("jvm") version "1.2.71"
}

repositories {
  jcenter()
}

dependencies {
  compile(group = "org.jetbrains.kotlin", name = "kotlin-stdlib")
  compile(group = "com.google.code.gson", name = "gson", version = "2.8.5")
  compile(group = "org.mongodb", name = "mongodb-driver-sync", version = "3.8.2")
  compile(group = "com.sparkjava", name = "spark-core", version = "2.7.2")
  compile(group = "com.sparkjava", name = "spark-kotlin", version = "1.0.0-alpha")
}
