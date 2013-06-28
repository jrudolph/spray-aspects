package spray.aspects

import org.aspectj.lang.annotation._

@Aspect()
class SprayAspects {
  @Before("execution(* akka.io.TcpConnection.doRead(..))")
  def gotya(): Unit = {
    println("Invoked")
  }
}
