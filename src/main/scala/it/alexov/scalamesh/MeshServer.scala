package it.alexov.scalamesh

import java.nio.file.{Files, Path, Paths}
import akka.{Done, NotUsed}
import akka.actor.{ActorRef, ActorSystem}
import akka.event.LoggingAdapter
import akka.stream.scaladsl.Tcp.{IncomingConnection, ServerBinding}
import akka.stream._
import akka.stream.scaladsl.{FileIO, Flow, Keep, RunnableGraph, Sink, Source, Tcp}
import akka.util.ByteString
import it.alexov.scalamesh.input.InputProvider
import it.alexov.scalamesh.util.{BroadcastActor, FutureUtils, RSA}
import scala.concurrent.duration._
import scala.concurrent.Future
import scala.util.{Failure, Success, Try}

class MeshServer(host: String, port: Int, filePathString: String, seedHostPort: Seq[(String,Int)], inputProvider: InputProvider)(implicit system: ActorSystem, materializer: ActorMaterializer, log: LoggingAdapter) {

  implicit val context = system.dispatcher
  implicit val scheduler = system.scheduler

  // Concurrency safe data structure
  final val chunkSize = 1024
  val pastTransactions: scala.collection.mutable.Map[ByteString, DataMessage] = scala.collection.concurrent.TrieMap.empty[ByteString,DataMessage]

  def checkIncomingMessage(msg: DataMessage): Boolean = {
    val sigSource = msg.timestamp.toEpochMilli.toString + msg.data
    val signatureCheck = RSA.verify(sigSource,msg.signature.toArray,msg.publicKey)
    if (!signatureCheck) {
      log.info(s"Discarding transaction on signature check: ${msg.ref}")
    }
    val alreadyInCheck = pastTransactions.contains(msg.signature)
    if (alreadyInCheck) {
      log.info(s"Discarding transaction already have it: ${msg.ref}")
    }
    val check = signatureCheck && !alreadyInCheck
    check
  }

  // -------------------------------------------------------------

  val seedConnections: Seq[((String, Int), Flow[ByteString, ByteString, Future[Tcp.OutgoingConnection]])] = seedHostPort.zip(seedHostPort.map(s => Tcp().outgoingConnection(s._1, s._2)))
  val inputBinding: Source[IncomingConnection, Future[ServerBinding]] = Tcp().bind(host, port)

  val readMessagesFlow: Flow[ByteString,Try[DataMessage],_] = Flow.fromFunction(DataMessage.decode)
  val writeMessagesFlow: Flow[DataMessage,ByteString,_] = Flow.fromFunction(_.encode())

  val filePath: Path = Paths.get(filePathString)
  val saveSink: Sink[ByteString, Future[IOResult]] = FileIO.toPath(filePath)

  val outputBroadcastActor: ActorRef = system.actorOf(BroadcastActor.props,BroadcastActor.actorName)

  // Elaborate new messages
  val messageFlow: Flow[DataMessage, ByteString, NotUsed] = Flow[DataMessage]
    .map(msg => {
      pastTransactions.put(msg.signature,msg)
      msg.encode()
    })
    .alsoTo(saveSink)
    .alsoTo(Sink.actorRef(outputBroadcastActor,None))

  // Elaborate messages from other nodes
  val inputFlow: Flow[ByteString, ByteString, NotUsed] = Flow[ByteString]
    .via(readMessagesFlow)
    .filter {
      case Success(msg) =>
        log.info(s"New incoming transaction: ${msg.ref}")
        true
      case Failure(e) =>
        e.printStackTrace()
        log.error("Bad input message arrived")
        false
    }
    .map(_.get)
    .filter(checkIncomingMessage)
    .via(messageFlow)

  // Connection handler
  val inputHandler: Sink[IncomingConnection, Future[Done]] = Sink.foreach[Tcp.IncomingConnection] { conn =>
    log.info("Node connected from: " + conn.remoteAddress)
    conn handleWith inputFlow
  }

  // Create new messages for seed nodes
  val dataProvider: RunnableGraph[_] = inputProvider.inputSource
    .map(msg => {
      log.info(s"Issued transaction: ${msg.ref}")
      msg
    })
    .via(messageFlow)
    .to(Sink.ignore)

  // Flow for each individual seed node
  val outputFlow: Flow[Any, ByteString, ActorRef] =
    Flow.fromSinkAndSourceMat(Sink.ignore,Source.actorRef[ByteString](2000,OverflowStrategy.fail))(Keep.right)

  // Bootstrap
  def run(): Unit = {
    inputBinding.to(inputHandler).run().onComplete {
      case Success(_) =>
        log.info(s"Node is up and waiting for incoming connections on $host:$port")
        dataProvider.run()
      case Failure(e) =>
        println(s"Node could not bind to $host:$port: ${e.getMessage}")
        system.terminate()
    }
    seedConnections.foreach(tuple => {
      val ((seedHost,seedPort),flow) = tuple
      log.info(s"Connecting to seed $seedHost:$seedPort")
      FutureUtils.retry(() => flow.joinMat(outputFlow)((f,ref) => f.map(_ => ref)).run(), 30 seconds, 20).onComplete {
        case Success(actorRef) =>
          log.info(s"Connected to seed on $seedHost:$seedPort")
          outputBroadcastActor ! BroadcastActor.Add(actorRef)
        case Failure(e) => log.info(s"Could not connect to seed $seedHost:$seedPort: ${e.getMessage}")
      }
    })
  }

}
