package softeng306.project2

import softeng306.project2.multiplayer.sockets.Multiplayer
import spark.Spark.init
import spark.Spark.webSocket
import spark.kotlin.port

fun main(vararg args: String) {
  port(8080)

  webSocket("/play", Multiplayer::class.java)

  init()
}
