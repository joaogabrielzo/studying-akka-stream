import akka.stream._
import akka.stream.scaladsl._
import akka.{Done, NotUsed}
import akka.actor.ActorSystem
import akka.util.ByteString

import scala.concurrent._
import scala.concurrent.duration._
import java.nio.file.Paths

import scala.util.{Failure, Success}

object StreamsQuickguide extends App {
    implicit val system = ActorSystem("QuickStart")
    implicit val materialzier = ActorMaterializer()

    val source: Source[Int, NotUsed] = (Source(1 to 100))

//    val runSource: Future[Done] = source.runForeach(i => println(i))
    implicit val ec: ExecutionContextExecutor = system.dispatcher
//
//    runSource.onComplete(_ => system.terminate())

    val factorials = source.scan(BigInt(1))((acc, next) => acc * next)

    val result: Future[IOResult] =
        factorials.map(num => ByteString(s"$num\n")).runWith(FileIO.toPath(Paths.get("/home/zo/Desktop/factorials.txt")))

//    result.onComplete {
//        case Success(x) =>
//            println("File saved")
//            system.terminate()
//        case Failure(exception) =>
//            println("Failed with ", exception)
//            system.terminate()
//    }

    def lineSink(filename: String): Sink[String, Future[IOResult]] =
        Flow[String].map(s => ByteString(s + "\n")).toMat(FileIO.toPath(Paths.get(filename)))(Keep.right)

//    factorials.map(_.toString).runWith(lineSink("/home/zo/Desktop/test.txt"))

    factorials
        .zipWith(Source(0 to 100))((num, idx) => s"$idx! = $num")
//        .throttle(1, 1.second)
        .runForeach(println)

}
