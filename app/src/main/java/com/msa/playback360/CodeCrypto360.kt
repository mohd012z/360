package com.msa.playback360

import java.security.MessageDigest
import javax.crypto.Cipher
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.PBEKeySpec
import javax.crypto.spec.SecretKeySpec
import java.security.SecureRandom

/**
 * Workspace crypto for data the user owns and explicitly supplies.
 * No key extraction, password recovery, protection bypass or DRM circumvention.
 */
object CodeCrypto360 {
 data class EncryptedBlob(val salt:ByteArray,val iv:ByteArray,val ciphertext:ByteArray)

 fun encrypt(data:ByteArray,password:CharArray):EncryptedBlob{
  val salt=ByteArray(16).also{SecureRandom().nextBytes(it)}
  val iv=ByteArray(12).also{SecureRandom().nextBytes(it)}
  val key=derive(password,salt)
  val cipher=Cipher.getInstance("AES/GCM/NoPadding")
  cipher.init(Cipher.ENCRYPT_MODE,key,GCMParameterSpec(128,iv))
  return EncryptedBlob(salt,iv,cipher.doFinal(data))
 }

 fun decrypt(blob:EncryptedBlob,password:CharArray):ByteArray{
  val key=derive(password,blob.salt)
  val cipher=Cipher.getInstance("AES/GCM/NoPadding")
  cipher.init(Cipher.DECRYPT_MODE,key,GCMParameterSpec(128,blob.iv))
  return cipher.doFinal(blob.ciphertext)
 }

 fun fingerprint(data:ByteArray)=MessageDigest.getInstance("SHA-256")
  .digest(data).joinToString(""){"%02x".format(it)}

 private fun derive(password:CharArray,salt:ByteArray):SecretKeySpec{
  val spec=PBEKeySpec(password,salt,120_000,256)
  val raw=SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256").generateSecret(spec).encoded
  spec.clearPassword()
  return SecretKeySpec(raw,"AES")
 }
}
