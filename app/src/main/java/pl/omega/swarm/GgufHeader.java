package pl.omega.swarm;
import java.io.*;import java.nio.*;import java.nio.charset.StandardCharsets;
/** Checks GGUF v2/v3 file headers only, not the full tensor payload. */
public final class GgufHeader {
 public final int version; public final long tensorCount,metadataEntries;
 private GgufHeader(int v,long n,long m){version=v;tensorCount=n;metadataEntries=m;}
 public static GgufHeader parse(InputStream in)throws IOException {
  byte[] h=in.readNBytes(24);
  if(h.length!=24)throw new IOException("GGUF header truncated");
  if(!"GGUF".equals(new String(h,0,4,StandardCharsets.US_ASCII)))throw new IOException("Invalid GGUF magic");
  int le=ByteBuffer.wrap(h,4,4).order(ByteOrder.LITTLE_ENDIAN).getInt();
  ByteOrder order=(le==2||le==3)?ByteOrder.LITTLE_ENDIAN:ByteOrder.BIG_ENDIAN;
  ByteBuffer b=ByteBuffer.wrap(h).order(order);
  int v=b.getInt(4);if(v!=2&&v!=3)throw new IOException("Unsupported GGUF version");
  long n=b.getLong(8),m=b.getLong(16);
  if(n<0||n>10_000_000||m<0||m>1_000_000)throw new IOException("Invalid GGUF counters");
  return new GgufHeader(v,n,m);
 }
}
