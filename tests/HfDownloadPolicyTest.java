import pl.omega.swarm.HuggingFaceDownloader;
public class HfDownloadPolicyTest{
 public static void main(String[] a){
  if(!HuggingFaceDownloader.shouldAppend(100,206,"bytes 100-200/1000"))throw new AssertionError("valid range");
  if(HuggingFaceDownloader.shouldAppend(100,206,"bytes 0-200/1000"))throw new AssertionError("bad range");
  if(HuggingFaceDownloader.shouldAppend(100,200,null))throw new AssertionError("range ignored");
  if(HuggingFaceDownloader.shouldAppend(0,206,"bytes 0-200/1000"))throw new AssertionError("unexpected partial");
  System.out.println("PASS HTTP resume policy");
 }
}
