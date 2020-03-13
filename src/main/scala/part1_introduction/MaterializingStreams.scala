package part1_introduction

import akka.actor.ActorSystem
import akka.stream.ActorMaterializer
import akka.stream.scaladsl.{Flow, Keep, RunnableGraph, Sink, Source}
import akka.{Done, NotUsed}

import scala.concurrent.{ExecutionContextExecutor, Future}
import scala.util.{Failure, Success}

object MaterializingStreams extends App {
    
    implicit val system = ActorSystem("materializing-streams")
    implicit val materializer = ActorMaterializer()
    
    val simpleGraph: RunnableGraph[NotUsed] = Source(1 to 10).to(Sink.foreach(println))
    val simpleMaterializedValue: NotUsed = simpleGraph.run()
    
    implicit val ec: ExecutionContextExecutor = system.dispatcher
    
    val source: Source[Int, NotUsed] = Source(1 to 10)
    val sink: Sink[Int, Future[Int]] = Sink.reduce[Int]((a, b) => a + b)
    val sumFuture: Future[Int] = source.runWith(sink)
//    sumFuture.onComplete {
//        case Success(value)     => println("The sum of all elements is ", value)
//        case Failure(exception) => println("Failed with ", exception)
//    }
    
    //    choosing materialized values
    val simpleSource = Source(1 to 10)
    val simpleFlow = Flow[Int].map(x => x + 1)
    val simpleSink = Sink.foreach[Int](println)
    val graph: RunnableGraph[Future[Done]] = simpleSource.viaMat(simpleFlow)(Keep.right).toMat(simpleSink)(Keep.right)
//    graph.run().onComplete {
//        case Success(value)     => println("finished")
//        case Failure(exception) => exception
//    }
//
////    sugars
//    Source(1 to 10).runWith(Sink.reduce[Int](_ + _)) // = source.to(Sink.reduce)(Keep.right)
//    Source(1 to 10).runReduce(_ + _) // same
//
////    backwards
//    Sink.foreach[Int](println).runWith(Source.single(42)) // = source().runWith(sink)
////    both ways
//    Flow[Int].map(_ * 2).runWith(simpleSource, simpleSink)
    
    /**
      * Exercises
      */
//    Return last element
val lastSource: Source[Int, NotUsed] = Source(1 to 10)
    val lastRun: Future[Int] = lastSource.runWith(Sink.last)
    lastRun.onComplete {
        case Success(x) => println("Last value: ", x)
        case Failure(e) => println("Failed with: ", e)
    }
    
    //    Total word count
    val sentenceSource = Source(List(
        "Akka is ok",
        "Materialized values are killing me",
        "I kinda like streams"
        ))
    val sentenceSink = Sink.fold[Int, String](0)((currentWord, newSentence) => currentWord + newSentence.split(" ")
                                                                                                        .length)
    val sentenceRun = sentenceSource.toMat(sentenceSink)(Keep.right).run()
    sentenceRun.onComplete {
        case Success(x) => println("Here's the word count: " + x)
    }
    
}
