package it.alexov

import java.math.BigInteger
import java.nio.ByteOrder
import java.security.{MessageDigest, PublicKey}
import java.time.Instant
import java.util.Base64

import akka.event.LoggingAdapter
import akka.util.ByteString
import caseapp.ExtraName
import it.alexov.scalamesh.util.RSA

import scala.util.Try

package object scalamesh {


  case class Options(
     @ExtraName("h")
     host: String = "127.0.0.1",
     @ExtraName("p")
     port: Int = 10274,
     @ExtraName("f")
     file: String = "/home/my/chain.raw",
     @ExtraName("s")
     seed: List[String] = List("127.0.0.1:10275","127.0.0.1:10276")
  )

  case class DataMessage(
     timestamp: Instant,
     data: String,
     publicKey: PublicKey,
     signature: ByteString
  ) {
    def encode(): ByteString ={
      implicit val byteOrder: ByteOrder = ByteOrder.BIG_ENDIAN
      val builder = ByteString.createBuilder
      val dataBytes = data.getBytes("UTF-8")
      val keyBytes = publicKey.getEncoded
      val len: Short = (10 + dataBytes.length + keyBytes.length + signature.length).toShort
      builder.putShort(len)
      builder.putLong(timestamp.toEpochMilli)
      builder.putShort(dataBytes.length.toShort)
      builder.putBytes(dataBytes)
      builder.putBytes(keyBytes)
      builder.putBytes(signature.toArray)
      builder.result()
    }
    def sha = String.format("%032x", new BigInteger(1, MessageDigest.getInstance("SHA-256").digest(data.getBytes("UTF-8")))).take(7)
    def ref = s"$sha"
  }

  object DataMessage {
    def decode(byteString: ByteString)(implicit log: LoggingAdapter) = Try({
      implicit val byteOrder: ByteOrder = ByteOrder.BIG_ENDIAN
      val iter = byteString.iterator
      val msgLength = iter.getShort
      val ts = Instant.ofEpochMilli(iter.getLong)
      val dataLength = iter.getShort
      val data = iter.getByteString(dataLength).utf8String
      val keyBytes = iter.getBytes(RSA.keyLength)
      val key = RSA.decodePublicKey(keyBytes)
      val signature = iter.getByteString(RSA.signatureLength)
      DataMessage(ts,data,key.get,signature)
    })
  }

}
