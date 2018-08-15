package it.alexov.scalamesh.input

import java.time.Instant

import akka.stream.scaladsl.Source
import akka.util.ByteString
import it.alexov.scalamesh
import it.alexov.scalamesh.DataMessage
import it.alexov.scalamesh.util.RSA
import it.alexov.scalamesh.util.RandomlyDelayedSource

import scala.concurrent.duration._
import scala.util.Random

class RandomInput extends InputProvider {

  val keyPair = RSA.getKeyPair
  val maxDelay = 20 seconds

  override def inputSource: Source[scalamesh.DataMessage, _] = Source.fromGraph(new RandomlyDelayedSource[DataMessage](maxDelay, () => {
    val date = Instant.now()
    val data = Random.nextString(256)
    val millis = date.toEpochMilli.toString
    // Put time in signature calculation to be resistant to duplicates
    val sigSource = millis + data
    val signature = ByteString(RSA.sign(sigSource,keyPair.getPrivate))
    val fakeSignature = signature.reverse
    // For testing purposes network will issue fake transactions time to time
    val sig = if(Random.nextInt(6) == 5) fakeSignature else signature
    DataMessage(date,data,keyPair.getPublic,sig)
  }))

}
