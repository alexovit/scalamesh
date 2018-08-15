package it.alexov.scalamesh.util

import java.security._
import java.security.spec.X509EncodedKeySpec

object RSA {

  final val keyAlgorithm = "RSA"
  final val keyLength = 94
  final val sigAlgorithm = "SHA256withRSA"
  final val signatureLength = 64

  def decodePublicKey(encodedKey: Array[Byte]): Option[PublicKey] = {
    scala.util.control.Exception.allCatch.opt {
      val spec = new X509EncodedKeySpec(encodedKey)
      val factory = KeyFactory.getInstance(keyAlgorithm)
      factory.generatePublic(spec)
    }
  }

  def getKeyPair: KeyPair = {
    val keyGen = KeyPairGenerator.getInstance("RSA")
    keyGen.initialize(512)
    keyGen.generateKeyPair
  }

  def sign(plainText: String, privateKey: PrivateKey): Array[Byte] = {
    val sig = Signature.getInstance(sigAlgorithm)
    sig.initSign(privateKey)
    sig.update(plainText.getBytes("UTF-8"))
    sig.sign
  }

  def verify(plainText: String, signature: Array[Byte], publicKey: PublicKey): Boolean = {
    val sig = Signature.getInstance(sigAlgorithm)
    sig.initVerify(publicKey)
    sig.update(plainText.getBytes("UTF-8"))
    sig.verify(signature)
  }

}
