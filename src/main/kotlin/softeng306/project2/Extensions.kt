package softeng306.project2

import java.util.Collections

fun <E> MutableList<E>.synchronized(): MutableList<E> {
  return Collections.synchronizedList(this)
}
