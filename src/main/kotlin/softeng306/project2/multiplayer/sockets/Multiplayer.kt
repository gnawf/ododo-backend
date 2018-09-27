package softeng306.project2.multiplayer.sockets

import com.google.gson.Gson
import org.eclipse.jetty.websocket.api.Session
import org.eclipse.jetty.websocket.api.WebSocketException
import org.eclipse.jetty.websocket.api.annotations.OnWebSocketClose
import org.eclipse.jetty.websocket.api.annotations.OnWebSocketConnect
import org.eclipse.jetty.websocket.api.annotations.OnWebSocketMessage
import org.eclipse.jetty.websocket.api.annotations.WebSocket
import softeng306.project2.models.Player
import softeng306.project2.multiplayer.messages.Chat
import softeng306.project2.multiplayer.messages.GameSync

@WebSocket
class Multiplayer {

  private val sessions: MutableMap<Session, String> = hashMapOf()

  private val players: MutableMap<String, Player> = hashMapOf()

  private val orphans: MutableSet<Session> = hashSetOf()

  private val gson = Gson()

  init {
    // Start the game processor in a separate thread
    Thread(this::ticker).start()
  }

  @OnWebSocketConnect
  fun connected(session: Session) {
    orphans.add(session)

    println("Orphan connected")
  }

  @OnWebSocketClose
  fun closed(session: Session, statusCode: Int, reason: String) {
    orphans.remove(session)

    val username = sessions.remove(session) ?: return
    players.remove(username)
  }

  @OnWebSocketMessage
  fun message(session: Session, message: String) {
    val type = message.substringBefore('\n')
    val body = message.substringAfter('\n')

    if (session in orphans) {
      // Ensure the first request is login
      if (type != "player-sync") {
        return session.close(-1, "Login required before continuing")
      }
      return login(session, body)
    }

    val request = fromJson(type, body)

    when (request) {
      is Player -> update(session, request)
    }
  }

  private fun fromJson(type: String, body: String): Any {
    val klass = when (type) {
      "player-sync" -> Player::class.java
      else -> throw UnsupportedOperationException("Unknown message type $type")
    }

    return gson.fromJson(body, klass)
  }

  private fun update(session: Session, player: Player) {
    val username = sessions[session] ?: return println("No session found for $player")
    players[username] = player

    println("Updated player $player")
  }

  private fun login(session: Session, body: String) {
    // Add the user
    val player = gson.fromJson(body, Player::class.java)
    sessions[session] = player.username
    players[player.username] = player
    orphans.remove(session)
    // Let the user know they have successfully logged in
    session.remote.sendString("success")

    println("Player logged in $player")
  }

  private fun ticker() {
    var start: Long

    while (true) {
      start = System.nanoTime()

      tick()

      // Sleep for the time remaining in the tick
      val sleep = 600000000 - (System.nanoTime() - start)
      // Make sure to convert back to milliseconds for Thread#sleep
      if (sleep >= 0) Thread.sleep(sleep / 1000000)
    }
  }

  private fun tick() {
    val messages = emptyList<Chat>()
    val sync = GameSync(iteration = 0, players = players.values, chat = messages)
    val message = "sync\n" + gson.toJson(sync)
    // Send to all sessions
    sessions.keys.forEach { session ->
      try {
        session.remote.sendStringByFuture(message)
      } catch (e: WebSocketException) {
        System.exit(1)
      }
    }
  }

}
