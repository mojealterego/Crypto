package pl.omega.swarm;
import java.io.*;import java.nio.charset.StandardCharsets;import java.nio.file.*;
import java.security.*;import java.util.*;import java.util.concurrent.atomic.AtomicBoolean;
/** Isolated app-private library; atomic publication after GGUF header check and SHA256. */
public final class ModelFiles {
 public static final long MAX_BYTES=64L*1024*1024*1024;
 public interface Progress{void onBytes(long n);}
 public static final class Entry {
  public final File file;public final String sha256;public final long bytes;
  Entry(File f,String h,long b){file=f;sha256=h;bytes=b;}
 }
 private final File folder;
 public ModelFiles(File f)throws IOException{if(!f.isDirectory()&&!f.mkdirs())throw new IOException("Cannot create vault");folder=f.getCanonicalFile();}
 public File directory(){return folder;}
 public static String displayName(String s){
  String n=s==null?"model.gguf":s.replace('\\','/');
  n=n.substring(n.lastIndexOf('/')+1).replaceAll("[^a-zA-Z0-9._-]","_");
  if(!n.toLowerCase(Locale.ROOT).endsWith(".gguf")||n.equals(".gguf")||n.length()>180)throw new IllegalArgumentException("Invalid GGUF filename");
  return n;
 }
 public Entry importFile(String name,InputStream input,AtomicBoolean cancelled,Progress progress)throws IOException{
  String safe=displayName(name);File tmp=new File(folder,UUID.randomUUID()+".part");long copied=0;
  try{
   try(OutputStream out=new BufferedOutputStream(new FileOutputStream(tmp))){byte[] buffer=new byte[262144];int n;
    while((n=input.read(buffer))!=-1){if(cancelled.get())throw new IOException("Cancelled");copied+=n;
     if(copied>MAX_BYTES)throw new IOException("64GiB limit");out.write(buffer,0,n);
     if(progress!=null)progress.onBytes(copied);}}
   if(cancelled.get())throw new IOException("Cancelled");
   return publish(tmp,safe,null);
  }finally{Files.deleteIfExists(tmp.toPath());}
 }
 public Entry publish(File tmp,String name,String expectedSha)throws IOException{
  String safe=displayName(name);
  if(!tmp.getCanonicalFile().getParentFile().equals(folder))throw new IOException("Path outside vault");
  if(!tmp.isFile()||tmp.length()<24||tmp.length()>MAX_BYTES)throw new IOException("Invalid model size");
  try(InputStream in=new BufferedInputStream(new FileInputStream(tmp))){GgufHeader.parse(in);}
  String sha=sha256(tmp);
  if(expectedSha!=null&&!sha.equalsIgnoreCase(expectedSha))throw new IOException("SHA256 mismatch");
  File target=new File(folder,UUID.randomUUID()+"--"+safe);
  try{
   try{Files.move(tmp.toPath(),target.toPath(),StandardCopyOption.ATOMIC_MOVE);}
   catch(AtomicMoveNotSupportedException e){Files.move(tmp.toPath(),target.toPath());}
   Files.writeString(new File(folder,target.getName()+".sha256").toPath(),sha,StandardCharsets.US_ASCII);
   return new Entry(target,sha,target.length());
  }catch(IOException e){Files.deleteIfExists(target.toPath());throw e;}
 }
 public static String sha256(File f)throws IOException{
  try{MessageDigest digest=MessageDigest.getInstance("SHA-256");
   try(InputStream in=new BufferedInputStream(new FileInputStream(f))){byte[] b=new byte[524288];int n;while((n=in.read(b))!=-1)digest.update(b,0,n);}
   StringBuilder out=new StringBuilder();for(byte b:digest.digest())out.append(String.format(Locale.ROOT,"%02x",b&255));return out.toString();
  }catch(NoSuchAlgorithmException e){throw new IOException(e);}
 }
 public List<Entry> list(){
  File[] a=folder.listFiles((dir,n)->n.endsWith(".gguf")&&n.contains("--"));if(a==null)return Collections.emptyList();
  Arrays.sort(a,(x,y)->Long.compare(y.lastModified(),x.lastModified()));List<Entry> out=new ArrayList<>();
  for(File f:a){String hash="unknown";try{File meta=new File(folder,f.getName()+".sha256");if(meta.isFile())hash=Files.readString(meta.toPath()).trim();}catch(IOException ignored){}
   out.add(new Entry(f,hash,f.length()));}return out;
 }
 public boolean delete(String name)throws IOException{
  if(name==null||!name.matches("[a-zA-Z0-9_-]{36}--[a-zA-Z0-9._-]{1,180}\\.gguf"))throw new IllegalArgumentException("Invalid file identifier");
  File f=new File(folder,name).getCanonicalFile();if(!f.getParentFile().equals(folder))throw new IOException("Invalid path");
  boolean removed=Files.deleteIfExists(f.toPath());Files.deleteIfExists(new File(folder,name+".sha256").toPath());return removed;
 }
}
