package renesca.benchmark

object Benchmark {
  def time(code: => Unit) = {
    def now = System.nanoTime
    val start = now
    code
    val end = now
    (end - start) / 1000000000.0
  }

  def benchmark(times: Int)(code: => Unit) = {
    time {
      for (i <- 0 until times)
        code
    } / times
  }
}
