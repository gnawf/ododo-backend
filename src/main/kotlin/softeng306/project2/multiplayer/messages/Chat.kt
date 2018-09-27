package softeng306.project2.multiplayer.messages

data class Chat constructor(
  private val owner: String,
  private val message: String
)
