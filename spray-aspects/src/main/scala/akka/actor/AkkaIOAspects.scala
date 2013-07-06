package akka.io

import akka.actor._
import org.aspectj.lang.annotation._
import org.aspectj.lang.ProceedingJoinPoint
import java.nio.channels.{SocketChannel, Channel}
import akka.io.SelectionHandler.ChannelReadable

@Aspect()
class AkkaIOAspects {
  @Around("execution(* akka.io.TcpConnection.receive(..)) && target(actor)")
  def gotya(thisJoinPoint: ProceedingJoinPoint, actor: akka.io.TcpConnection): Actor.Receive = {
    val handler = thisJoinPoint.proceed.asInstanceOf[Actor.Receive]
    println(s"Got handler '${handler.getClass}'")

    {
      case x@ChannelReadable =>
        IOLogHandler.handler.handleChannelReadable(actor.context.self, actor.channel)
        handler(x)
      case x@_ if handler.isDefinedAt(x) =>
        println(s"Got '$x' , ctx is ${actor.context}")
        handler(x)
    }
  }

  @Around("within(akka.io.TcpConnection) && call(* akka.actor.ActorContext.become(scala.PartialFunction, boolean)) && args(newReceive, discardOld)")
  def captureBecome(thisJoinPoint: ProceedingJoinPoint, newReceive: Actor.Receive, discardOld: Boolean): Unit = {
    println(s"Captured become ${newReceive.getClass} discard: $discardOld")
    val ctx = thisJoinPoint.getTarget.asInstanceOf[ActorContext]
    val self = ctx.self
    val actor = ctx.asInstanceOf[ActorCell].actor.asInstanceOf[TcpConnection]
    thisJoinPoint.proceed(Array(
      {
        case x@ChannelReadable =>
          IOLogHandler.handler.handleChannelReadable(self, actor.channel)
          newReceive(x)
        case x@_ if newReceive.isDefinedAt(x) =>
          println(s"Fished '$x' for $self")
          newReceive(x)
      }: Actor.Receive, discardOld: java.lang.Boolean))
  }

  @Before("within(akka.io.TcpConnection) && " +
    "call(* akka.actor.ScalaActorRef.$bang(Object, akka.actor.ActorRef)) && target(recv) && args(msg, sender)")
  def msgSends(recv: ActorRef, msg: Any, sender: ActorRef): Unit = {
    println(s"Sent $msg to $recv")
  }
}

trait IOLogHandler {
  def handleChannelReadable(connection: ActorRef, channel: SocketChannel): Unit
}

object IOLogHandler {
  var handler: IOLogHandler = new IOLogHandler {
    def handleChannelReadable(connection: ActorRef, channel: SocketChannel) {}
  }

  def setHandler(handler: IOLogHandler): Unit = this.handler = handler
}
