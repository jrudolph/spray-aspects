package spray.io

import org.aspectj.lang.annotation.{Before, Around, Aspect}
import org.aspectj.lang.ProceedingJoinPoint
import akka.actor.Actor
import java.net.InetSocketAddress

case class Connection(id: Int, timestamp: Long, remote: InetSocketAddress, local: InetSocketAddress)
case class Interaction()

@Aspect()
class PipelineAspects {
  //@Around("execution(* spray.can.client.HttpClientConnection.running(..)) && target(actor)")
  def receiveHandler(thisJoinPoint: ProceedingJoinPoint, actor: ConnectionHandler): Actor.Receive = {
    val handler = thisJoinPoint.proceed.asInstanceOf[Actor.Receive]
    println(s"Got handler '${handler.getClass}'")

    {
      case x@_ if handler.isDefinedAt(x) =>
        println(s"[pipeline] Got '$x' , ctx is ${actor}")
        handler(x)
    }
  }

  @Around("within(spray.can.client.HttpClientConnection$) && call(* spray.io.RawPipelineStage.$greater$greater(spray.io.RawPipelineStage)) && target(thisStage) && args(nextStage)")
  def pipelineCombinator(
    thisJoinPoint: ProceedingJoinPoint,
    thisStage: RawPipelineStage[PipelineContext],
    nextStage: RawPipelineStage[PipelineContext]): AnyRef = {

    nextStage match {
      case EmptyPipelineStage => return thisStage
      case _ =>
    }

    println(s"Got combinator: $thisStage >> $nextStage")
    val nextClass = nextStage.getClass
    def prefix(dir: String) = s"[${thisStage.getClass.getSimpleName} $dir ${Option(nextClass.getEnclosingClass).getOrElse(nextClass).getSimpleName}]"
    val stage = new PipelineStage {
      def apply(context: PipelineContext, commandPL: CPL, eventPL: EPL): Pipelines = new Pipelines {
        def commandPipeline: CPL = {
          case c =>
            println(s"${Console.BLUE}C${Console.RESET} ${prefix("=>")}: $c")
            commandPL(c)
        }

        def eventPipeline: EPL = {
          case TickGenerator.Tick => // ignore1
          case e =>
            println(s"${Console.GREEN}E${Console.RESET} ${prefix("<=")}: $e")
            eventPL(e)
        }
      }
    }

    val res =
      thisStage >> stage >> nextStage
    thisJoinPoint.proceed(
      Array[AnyRef](thisStage, nextStage)
    )
    println(s"Combined is $res")
    res
  }
}

