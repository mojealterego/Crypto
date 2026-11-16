import pl.omega.swarm.*;import java.io.*;import java.nio.file.*;import java.nio.charset.StandardCharsets;
import java.util.*;import java.util.concurrent.atomic.AtomicBoolean;
public class GgufLibraryTest{
 static void check(boolean ok,String s){if(!ok)throw new AssertionError(s);}
 static void reject(Job work)throws Exception{try{work.run();throw new AssertionError("Expected reject");}catch(IOException|IllegalArgumentException expected){}}
 interface Job{void run()throws Exception;}
 static byte[] header()throws Exception{
  ByteArrayOutputStream out=new ByteArrayOutputStream();out.write("GGUF".getBytes(StandardCharsets.US_ASCII));
  for(int b:new int[]{3,0,0,0,5,0,0,0,0,0,0,0,1,0,0,0,0,0,0,0})out.write(b);
  return out.toByteArray();
 }
 public static void main(String[] args)throws Exception{
  byte[] b=header();GgufHeader parsed=GgufHeader.parse(new ByteArrayInputStream(b));
  check(parsed.version==3&&parsed.tensorCount==5&&parsed.metadataEntries==1,"valid GGUF");
  reject(()->GgufHeader.parse(new ByteArrayInputStream(new byte[]{1,2,3})));
  reject(()->GgufHeader.parse(new ByteArrayInputStream("NOT_A_GGUF".getBytes(StandardCharsets.US_ASCII))));
  check(HuggingFacePaths.resolve("org/repo","main","model-Q4.gguf").startsWith("https://huggingface.co/"),"resolve HTTPS");
  reject(()->HuggingFacePaths.resolve("org/../bad","main","bad.gguf"));
  reject(()->HuggingFacePaths.resolve("org/repo","main","../bad.gguf"));
  check(!HuggingFacePaths.trustedRedirect("https://hf.co.evil.com/file"),"deny malicious redirect");
  check(!HuggingFacePaths.trustedRedirect("http://huggingface.co/model"),"deny HTTP");
  File tmp=Files.createTempDirectory("omega-gguf-test-").toFile();
  try{ModelFiles store=new ModelFiles(tmp);
   ModelFiles.Entry entry=store.importFile("test.gguf",new ByteArrayInputStream(b),new AtomicBoolean(false),null);
   check(entry.file.exists()&&entry.sha256.length()==64,"copied with SHA");
   check(store.list().size()==1,"listed");
   reject(()->store.importFile("bad.gguf",new ByteArrayInputStream(new byte[]{1,2,3}),new AtomicBoolean(false),null));
   check(store.list().size()==1,"invalid not published");
   reject(()->store.delete("../bad.gguf"));
   check(store.delete(entry.file.getName()),"delete");
  }finally{for(File f:Objects.requireNonNull(tmp.listFiles()))f.delete();tmp.delete();}
  System.out.println("PASS GGUF and Hugging Face policy");
 }
}
