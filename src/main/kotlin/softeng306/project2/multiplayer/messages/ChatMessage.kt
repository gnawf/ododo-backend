package softeng306.project2.multiplayer.messages

data class ChatMessage constructor(
  val id: Int,
  val owner: String,
  val message: String,
  val sentAt: String
)
