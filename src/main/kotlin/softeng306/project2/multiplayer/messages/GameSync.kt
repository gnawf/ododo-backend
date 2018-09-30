package softeng306.project2.multiplayer.messages

import softeng306.project2.models.Player

data class GameSync constructor(
  private val lastChatMessageId: Int,
  private val players: Collection<Player>
)
