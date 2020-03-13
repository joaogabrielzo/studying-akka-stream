package part2_graphs

import akka.actor.ActorSystem
import akka.stream.{ActorMaterializer, BidiShape, ClosedShape}
import akka.stream.scaladsl.{Flow, GraphDSL, RunnableGraph, Sink, Source}

object BidirectionalFlows extends App {

    implicit val system: ActorSystem = ActorSystem("bidirectional-flows")
    implicit val materializer: ActorMaterializer = ActorMaterializer()

    def encrypt(n: Int)(str: String) = str.map(c => (c + n).toChar)
    def decrypt(n: Int)(str: String) = str.map(c => (c - n).toChar)

    val bidirectionalFlow = GraphDSL.create() { implicit builder =>
        import GraphDSL.Implicits._

        val encryptionFlowShape = builder.add(Flow[String].map(encrypt(3)))
        val decryptionFlowShape = builder.add(Flow[String].map(decrypt(3)))

//        BidiShape(encryptionFlowShape.in, encryptionFlowShape.out, decryptionFlowShape.in, decryptionFlowShape.out)
        BidiShape.fromFlows(encryptionFlowShape, decryptionFlowShape)
    }

    val unencryptedStrings = List("only", "three", "strings")
    val unencryptedSource = Source(unencryptedStrings)
    val encryptedSource = Source(unencryptedStrings.map(encrypt(3)))

    val cryptoBidiGraph = RunnableGraph.fromGraph(
        GraphDSL.create() { implicit builder =>
            import GraphDSL.Implicits._

            val unencryptedSourceShape = builder.add(unencryptedSource)
            val encryptedSourceShape = builder.add(encryptedSource)
            val bidi = builder.add(bidirectionalFlow)
            val encryptedSink = builder.add(Sink.foreach[String](x => println(s"Encrypted: $x")))
            val decryptedSink = builder.add(Sink.foreach[String](x => println(s"Decrypted: $x")))

            unencryptedSourceShape ~> bidi.in1
            bidi.out1 ~> encryptedSink

            decryptedSink <~ bidi.out2
            bidi.in2 <~ encryptedSourceShape

            ClosedShape
        }
    )

    cryptoBidiGraph.run()

    /**
      * USED FOR
      * - encrypt/decrypt
      * - serialize/deserialize
      * - encoding/decoding
      */

}
