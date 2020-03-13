package SideProjects

import akka.actor.ActorSystem
import akka.stream.ActorMaterializer
import akka.stream.scaladsl.{Keep, RunnableGraph, Sink, Source}

import scala.concurrent.{ExecutionContextExecutor, Future}
import scala.util.{Failure, Success}


object LearningStreams extends App {

    implicit val system = ActorSystem("testing-streams")
    implicit val materializer: ActorMaterializer = ActorMaterializer()

    implicit val ec: ExecutionContextExecutor = system.dispatcher

    val source = Source(List(1, 2, 3, 4, 5, 6, 6, 7, 8, 9, 6,3,2 ,2 ,123,131,23,44,134,113))
    val sink = Sink.fold[Int, Int](0)(_ + _)

    val runnable: RunnableGraph[Future[Int]] = source.toMat(sink)(Keep.right)

    val sum: Future[Int] = runnable.run()

    sum.onComplete {
        case Success(x) => println(x)
        case Failure(exception) => println(s"Failed with $exception")
    }
}

