package softeng306.project2

import java.util.Collections

fun <E> MutableList<E>.synchronized(): MutableList<E> {
  return Collections.synchronizedList(this)
}

fun <E> MutableIterator<E>.forEach(consumer: (MutableIterator<E>, E) -> Unit) {
  while (hasNext()) {
    val item = next()
    consumer.invoke(this, item)
  }
}
