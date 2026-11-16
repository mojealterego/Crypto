package pl.omega.swarm;
import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.nio.charset.StandardCharsets;
public final class CommandCodec {
 private CommandCodec() {}
 private static void string(DataOutputStream out,String s)throws Exception{
  byte[] b=s.getBytes(StandardCharsets.UTF_8);
  if(b.length>512)throw new IllegalArgumentException("Field too long");
  out.writeShort(b.length);out.write(b);
 }
 public static byte[] canonical(String nodeId,long timestampMs,String nonce,String action,double voltage,double frequency,String pool)throws Exception{
  if(!nodeId.matches("[a-zA-Z0-9_-]{1,64}"))throw new IllegalArgumentException("Invalid node");
  if(!nonce.matches("[0-9a-f-]{36}"))throw new IllegalArgumentException("Invalid nonce");
  if(!(action.equals("STOP")||action.equals("POOL")||action.equals("DVFS")))throw new IllegalArgumentException("Invalid action");
  if(!Double.isFinite(voltage)||!Double.isFinite(frequency))throw new IllegalArgumentException("Non-finite control");
  ByteArrayOutputStream b=new ByteArrayOutputStream();DataOutputStream d=new DataOutputStream(b);
  d.write(new byte[]{'O','M','E','G','A',1});string(d,nodeId);d.writeLong(timestampMs);string(d,nonce);string(d,action);
  d.writeDouble(voltage);d.writeDouble(frequency);string(d,pool);d.flush();return b.toByteArray();
 }
}
