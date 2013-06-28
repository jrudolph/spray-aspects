package spray.examples

import org.aspectj.lang.annotation._

@Aspect()
class MyAspect {
  @Before("execution(* akka.io.TcpConnection.doRead(..))")
  def gotya(): Unit = {
    println("Invoked")
  }
}
