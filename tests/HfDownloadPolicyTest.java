import pl.omega.swarm.HuggingFaceDownloader;
public class HfDownloadPolicyTest{
 public static void main(String[] a){
  if(!HuggingFaceDownloader.shouldAppend(100,206,"bytes 100-200/1000"))throw new AssertionError("valid range");
  if(HuggingFaceDownloader.shouldAppend(100,206,"bytes 0-200/1000"))throw new AssertionError("bad range");
  if(HuggingFaceDownloader.shouldAppend(100,200,null))throw new AssertionError("range ignored");
  if(HuggingFaceDownloader.shouldAppend(0,206,"bytes 0-200/1000"))throw new AssertionError("unexpected partial");
  if(HuggingFaceDownloader.shouldAppend(1024,206,"bytes 1024-8192/8192"))throw new AssertionError("end outside object");
  if(HuggingFaceDownloader.shouldAppend(1024,206,"bytes 1024-4095/*"))throw new AssertionError("unknown object length");
  if(HuggingFaceDownloader.shouldAppend(1024,206,"bytes 1024-4095/9223372036854775808"))throw new AssertionError("overflow");
  if(HuggingFaceDownloader.rangeTotal(1024,"bytes 1024-4095/8192")!=8192)throw new AssertionError("whole object length, not chunk length");
  if(HuggingFaceDownloader.rangeTotal(1024,"bytes 1024-4095/68719476737")!=-1)throw new AssertionError("object exceeds cap");
  System.out.println("PASS HTTP resume policy");
 }
}
