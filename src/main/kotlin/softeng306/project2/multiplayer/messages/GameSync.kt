package softeng306.project2.multiplayer.messages

import softeng306.project2.models.Player

data class GameSync constructor(
  private val iteration: Int,
  private val players: Collection<Player>,
  private val chat: List<Chat>
)
