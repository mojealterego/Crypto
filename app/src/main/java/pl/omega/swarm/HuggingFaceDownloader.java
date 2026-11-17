package pl.omega.swarm;
import java.io.*;import java.net.*;import java.nio.charset.StandardCharsets;import java.nio.file.Files;
import java.security.MessageDigest;import java.util.*;import java.util.concurrent.atomic.AtomicBoolean;
/** Streaming HTTPS model download with strict redirect allowlist, controlled resume and staging. */
public final class HuggingFaceDownloader {
 private static final int REDIRECT_LIMIT=8,TIMEOUT_MS=30000;
 public interface Progress{void onBytes(long received,long total);}
 private final ModelFiles vault;
 public HuggingFaceDownloader(ModelFiles v){vault=v;}
 public ModelFiles.Entry download(String repo,String revision,String path,String token,String expectedSha,AtomicBoolean cancel,Progress progress)throws IOException{
  String url=HuggingFacePaths.resolve(repo,revision,path);
  if(expectedSha!=null&&!expectedSha.isBlank()&&!expectedSha.matches("(?i)[0-9a-f]{64}"))throw new IllegalArgumentException("Expected SHA-256 must be 64 hex");
  String key=key(url);File tmp=new File(vault.directory(),"hf-"+key+".part"), origin=new File(vault.directory(),"hf-"+key+".origin");
  if(origin.isFile()&&!Files.readString(origin.toPath()).equals(url))Files.deleteIfExists(tmp.toPath());
  Files.writeString(origin.toPath(),url);
  // Mutable refs can change while a download is paused. Resume only at a pinned commit SHA.
  if(!revision.matches("(?i)[0-9a-f]{40}"))Files.deleteIfExists(tmp.toPath());
  long start=tmp.isFile()?tmp.length():0;HttpURLConnection c=null;
  try{
   c=open(url,token,start);int status=c.getResponseCode();
   if(status==416&&start>0){c.disconnect();Files.deleteIfExists(tmp.toPath());start=0;c=open(url,token,0);status=c.getResponseCode();}
   if(status!=200&&status!=206)throw new IOException("HF HTTP "+status);
   boolean append=shouldAppend(start,status,c.getHeaderField("Content-Range"));
   if(status==206&&!append)throw new IOException("Invalid Content-Range");
   if(!append)start=0;
   long length=c.getContentLengthLong(),total=append?rangeTotal(start,c.getHeaderField("Content-Range")):length;
   if(total>ModelFiles.MAX_BYTES)throw new IOException("64 GiB limit");
   if(total>0&&vault.directory().getUsableSpace()<total-start+32L*1024*1024)throw new IOException("Not enough storage");
   try(InputStream in=new BufferedInputStream(c.getInputStream());FileOutputStream out=new FileOutputStream(tmp,append)){
    byte[] buf=new byte[262144];long done=start;int n;
    while((n=in.read(buf))!=-1){if(cancel.get())throw new InterruptedIOException("Cancelled");
     done+=n;if(done>ModelFiles.MAX_BYTES)throw new IOException("64 GiB limit");
     out.write(buf,0,n);if(progress!=null)progress.onBytes(done,total);}
    out.getFD().sync();
   }
   if(total>=0&&tmp.length()!=total)throw new IOException("Incomplete transfer; retry to resume");
   if(cancel.get())throw new InterruptedIOException("Cancelled");
   ModelFiles.Entry result=vault.publish(tmp,path,expectedSha==null||expectedSha.isBlank()?null:expectedSha);
   Files.deleteIfExists(origin.toPath());return result;
  }finally{if(c!=null)c.disconnect();if(cancel.get()){Files.deleteIfExists(tmp.toPath());Files.deleteIfExists(origin.toPath());}}
 }
 public static boolean shouldAppend(long already,int status,String range) {
  if(already<=0||status!=206||range==null)return false;
  return rangeTotal(already,range)>0;
 }
 public static long rangeTotal(long offset,String range){
  if(offset<=0||range==null||!range.matches("bytes "+offset+"-[0-9]+/[0-9]+"))return -1;
  try{
   long end=Long.parseLong(range.substring(range.indexOf('-')+1,range.indexOf('/')));
   long total=Long.parseLong(range.substring(range.indexOf('/')+1));
   return end>=offset&&end<total&&total<=ModelFiles.MAX_BYTES?total:-1;
  }catch(NumberFormatException e){return -1;}
 }
 private static String key(String value)throws IOException{
  try{byte[] b=MessageDigest.getInstance("SHA-256").digest(value.getBytes(StandardCharsets.UTF_8));
   StringBuilder s=new StringBuilder();for(int i=0;i<12;i++)s.append(String.format(Locale.ROOT,"%02x",b[i]&255));return s.toString();
  }catch(Exception e){throw new IOException(e);}
 }
 private static HttpURLConnection open(String uri,String token,long start)throws IOException{
  String next=uri;
  for(int count=0;count<=REDIRECT_LIMIT;count++){
   if(!HuggingFacePaths.trustedRedirect(next))throw new IOException("Untrusted HF redirect");
   URI parsed=URI.create(next);
   HttpURLConnection c=(HttpURLConnection)parsed.toURL().openConnection();
   c.setInstanceFollowRedirects(false);c.setConnectTimeout(TIMEOUT_MS);c.setReadTimeout(TIMEOUT_MS);
   c.setRequestProperty("Accept-Encoding","identity");c.setRequestProperty("User-Agent","OMEGA-Swarm-Android/1.1");
   if(start>0)c.setRequestProperty("Range","bytes="+start+"-");
   if("huggingface.co".equalsIgnoreCase(parsed.getHost())&&token!=null&&!token.isBlank())c.setRequestProperty("Authorization","Bearer "+token.trim());
   int status=c.getResponseCode();
   if(status==301||status==302||status==303||status==307||status==308){
    String location=c.getHeaderField("Location");c.disconnect();if(location==null)throw new IOException("Missing redirect Location");
    next=parsed.resolve(location).toString();continue;
   }return c;
  }throw new IOException("Too many redirects");
 }
}
