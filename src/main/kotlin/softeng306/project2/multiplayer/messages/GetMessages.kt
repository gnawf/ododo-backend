package softeng306.project2.multiplayer.messages

data class GetMessages constructor(
  val sinceMessageId: Int,
  val limit: Int,
  val messages: List<ChatMessage>
)
