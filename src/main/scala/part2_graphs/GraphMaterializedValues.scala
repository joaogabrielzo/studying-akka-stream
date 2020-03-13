package part2_graphs

import akka.actor.ActorSystem
import akka.stream.{ActorMaterializer, FlowShape, SinkShape}
import akka.stream.scaladsl.{Broadcast, Flow, GraphDSL, Keep, Sink, Source}

import scala.concurrent.Future
import scala.util.{Failure, Success}

object GraphMaterializedValues extends App {

    implicit val system: ActorSystem = ActorSystem("graph-materialized-values")
    implicit val materializer: ActorMaterializer = ActorMaterializer()

    val wordSource = Source(List("Akka", "is", "an", "ok", "framework"))
    val printer = Sink.foreach[String](println)
    val counter = Sink.fold[Int, String](0)((count, _) => count + 1)

    /*
        Composite Sink Component
        - prints lowercase words
        - count short words (< 5)
     */

    val complexWordSink = Sink.fromGraph(
        GraphDSL.create(printer, counter)((printerMatValue, counterMatValue) => counterMatValue) {
            implicit builder => (printerShape, counterShape) =>
                import GraphDSL.Implicits._

                val broadcast = builder.add(Broadcast[String](2))
                val lowerFilter = builder.add(Flow[String].filter(x => x == x.toLowerCase))
                val shortFilter = builder.add(Flow[String].filter(x => x.length < 5))

                broadcast ~> lowerFilter ~> printerShape
                broadcast ~> shortFilter ~> counterShape

                SinkShape(broadcast.in)
        }
    )

    import system.dispatcher
//    val complexSinkFuture = wordSource.toMat(complexWordSink)(Keep.right).run()
//    complexSinkFuture.onComplete {
//        case Success(count) => println(s"Count is $count")
//        case Failure(exception) => println(s"Failed with $exception")
//    }

    /*
        Exercise
     */

    def enhanceFlow[A, B](flow: Flow[A, B, _]): Flow[A, B, Future[Int]] = {
        val counterSink = Sink.fold[Int, B](0)((count, _) => count + 1)

        Flow.fromGraph(
            GraphDSL.create(counterSink) { implicit builder => counterSinkShape =>
                import GraphDSL.Implicits._

                val broadcast = builder.add(Broadcast[B](2))
                val ogFlowShape = builder.add(flow)

                ogFlowShape ~> broadcast ~> counterSinkShape

                FlowShape(ogFlowShape.in, broadcast.out(1))
            }
        )
    }

    val simpleSource = Source(1 to 42)
    val simpleFlow = Flow[Int].map(x => x)
    val simpleSink = Sink.ignore

    val enhancedFlowCountFuture = simpleSource.viaMat(enhanceFlow(simpleFlow))(Keep.right).toMat(simpleSink)(Keep.left).run()
    enhancedFlowCountFuture.onComplete {
        case Success(value) => println(s"$value elements were enhanced")
        case Failure(exception) => println(s"failed with $exception")
    }

}
