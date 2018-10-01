package softeng306.project2.multiplayer.sockets

import com.google.gson.Gson
import org.eclipse.jetty.websocket.api.Session
import org.eclipse.jetty.websocket.api.WebSocketException
import org.eclipse.jetty.websocket.api.annotations.OnWebSocketClose
import org.eclipse.jetty.websocket.api.annotations.OnWebSocketConnect
import org.eclipse.jetty.websocket.api.annotations.OnWebSocketMessage
import org.eclipse.jetty.websocket.api.annotations.WebSocket
import softeng306.project2.forEach
import softeng306.project2.models.Player
import softeng306.project2.multiplayer.messages.ChatMessage
import softeng306.project2.multiplayer.messages.GameSync
import softeng306.project2.multiplayer.messages.GetMessages
import java.time.Instant

@WebSocket
class Multiplayer {

  private val tickDuration = System.getenv("TICK_DURATION_NANO").toLong()

  private val sessions: MutableMap<Session, String> = hashMapOf()

  private val players: MutableMap<String, Player> = hashMapOf()

  private val orphans: MutableSet<Session> = hashSetOf()

  private val messages: MutableList<ChatMessage> = arrayListOf()

  private val gson = Gson()

  init {
    // Start the game processor in a separate thread
    Thread(this::ticker).start()
  }

  @OnWebSocketConnect
  fun connected(session: Session) {
    orphans += session

    println("Orphan connected")
  }

  @OnWebSocketClose
  fun closed(session: Session, statusCode: Int, reason: String) {
    orphans -= session

    val username = sessions.remove(session) ?: return println("Orphan connection closed")
    players.remove(username)

    println("User disconnected $username")
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
      is ChatMessage -> store(session, request)
      is GetMessages -> send(session, request)
    }
  }

  private fun fromJson(type: String, body: String): Any {
    val klass = when (type) {
      "player-sync" -> Player::class.java
      "send-chat-message" -> ChatMessage::class.java
      "get-messages" -> GetMessages::class.java
      else -> throw UnsupportedOperationException("Unknown message type $type")
    }

    return gson.fromJson(body, klass)
  }

  private fun update(session: Session, player: Player) {
    val username = sessions[session] ?: return println("No session found for $player")
    players[username] = player

    println("Updated player $player")
  }

  private fun store(session: Session, request: ChatMessage) {
    val lastId = messages.lastOrNull()?.id ?: 0

    val message = request.copy(
      id = lastId + 1,
      owner = sessions[session] ?: return println("Unable to send chat message due to unknown player"),
      sentAt = Instant.now().toString()
    )

    messages += message

    println("Received message $message")
  }

  private fun send(session: Session, request: GetMessages) {
    // Grab the messages requested for
    val maxIndex = messages.size.minus(1).coerceAtLeast(0)
    val start = request.sinceMessageId.coerceIn(0, maxIndex)
    val limit = request.limit.coerceAtMost(20).takeIf { it > 0 } ?: 10
    val end = start.plus(limit).coerceAtMost(messages.size)
    val messages = messages.subList(start, end)

    // Send the response
    val response = request.copy(sinceMessageId = start, limit = limit, messages = messages)
    val payload = "get-messages\n" + gson.toJson(response)
    session.remote.sendStringByFuture(payload)

    println("Sent messages to ${sessions[session]} with payload $payload")
  }

  private fun login(session: Session, body: String) {
    // Add the user
    val player = gson.fromJson(body, Player::class.java)
    sessions[session] = player.username
    players[player.username] = player
    orphans.remove(session)

    println("Player logged in $player")
  }

  private fun ticker() {
    var start: Long
    var sleep: Long

    while (true) {
      start = System.nanoTime()

      tick()

      // Sleep for the time remaining in the tick
      sleep = tickDuration - (System.nanoTime() - start)
      // Make sure to convert back to milliseconds for Thread#sleep
      if (sleep >= 0) Thread.sleep(sleep / 1000000)
    }
  }

  private fun tick() {
    val sync = GameSync(
      players = players.values,
      lastChatMessageId = messages.lastOrNull()?.id ?: 0
    )
    val payload = "sync\n" + gson.toJson(sync)
    // Send to all sessions
    sessions.entries.iterator().forEach { iterator, entry ->
      val session = entry.key
      val username = entry.value
      try {
        session.remote.sendStringByFuture(payload)
      } catch (e: WebSocketException) {
        println("Removing user $username")
        iterator.remove()
        players.remove(username)
      }
    }
  }

}
