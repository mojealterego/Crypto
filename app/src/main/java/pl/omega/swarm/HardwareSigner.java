package pl.omega.swarm;
import android.content.Context;
import android.content.pm.PackageManager;
import android.security.keystore.*;
import java.security.*;
import java.security.spec.ECGenParameterSpec;
public final class HardwareSigner {
 private static final String ALIAS="omega_swarm_operator_p256_v1";
 private final Context context;
 public HardwareSigner(Context context){this.context=context;}
 private KeyStore store() throws Exception{KeyStore s=KeyStore.getInstance("AndroidKeyStore");s.load(null);return s;}
 public byte[] publicKey()throws Exception{ensure();return store().getCertificate(ALIAS).getPublicKey().getEncoded();}
 public Signature prepare()throws Exception{
  ensure();Signature s=Signature.getInstance("SHA256withECDSA");
  s.initSign((PrivateKey)store().getKey(ALIAS,null));return s;
 }
 private void ensure()throws Exception{
  if(!store().containsAlias(ALIAS)){
   if(!context.getPackageManager().hasSystemFeature(PackageManager.FEATURE_STRONGBOX_KEYSTORE))
    throw new SecurityException("StrongBox required; remote control disabled");
   KeyGenParameterSpec spec=new KeyGenParameterSpec.Builder(ALIAS,KeyProperties.PURPOSE_SIGN)
    .setAlgorithmParameterSpec(new ECGenParameterSpec("secp256r1"))
    .setDigests(KeyProperties.DIGEST_SHA256)
    .setUserAuthenticationRequired(true)
    .setUserAuthenticationParameters(0,KeyProperties.AUTH_BIOMETRIC_STRONG)
    .setInvalidatedByBiometricEnrollment(true)
    .setIsStrongBoxBacked(true).build();
   KeyPairGenerator kpg=KeyPairGenerator.getInstance(KeyProperties.KEY_ALGORITHM_EC,"AndroidKeyStore");
   try{kpg.initialize(spec);kpg.generateKeyPair();}
   catch(StrongBoxUnavailableException e){throw new SecurityException("StrongBox unavailable",e);}
  }
  PrivateKey key=(PrivateKey)store().getKey(ALIAS,null);
  KeyInfo info=KeyFactory.getInstance(key.getAlgorithm(),"AndroidKeyStore").getKeySpec(key,KeyInfo.class);
  if(info.getSecurityLevel()!=KeyProperties.SECURITY_LEVEL_STRONGBOX)
   throw new SecurityException("Key not backed by StrongBox");
 }
}
