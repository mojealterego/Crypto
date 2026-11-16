package pl.omega.swarm;
import java.net.URI;import java.net.URLEncoder;import java.nio.charset.StandardCharsets;import java.util.Locale;
/** Rejects path traversal and unapproved redirect destinations. */
public final class HuggingFacePaths {
 private HuggingFacePaths(){}
 private static String segment(String s){
  if(!s.matches("[a-zA-Z0-9][a-zA-Z0-9._-]{0,127}")||s.equals("..")||s.endsWith("."))throw new IllegalArgumentException("Bad HF identifier");
  return s;
 }
 public static String repo(String s){String[] a=s.split("/",-1);if(a.length!=2)throw new IllegalArgumentException("Expected owner/repo");return segment(a[0])+"/"+segment(a[1]);}
 private static String enc(String s){return URLEncoder.encode(s,StandardCharsets.UTF_8).replace("+","%20");}
 public static String path(String s){
  if(s==null||s.length()>1024||s.startsWith("/")||s.indexOf('\\')>=0)throw new IllegalArgumentException("Bad HF path");
  String[] a=s.split("/",-1);if(a.length>12)throw new IllegalArgumentException("Too many components");
  StringBuilder b=new StringBuilder();
  for(String p:a){if(p.isEmpty()||p.equals(".")||p.equals("..")||p.contains("%"))throw new IllegalArgumentException("Unsafe HF path");
   if(b.length()>0)b.append('/');b.append(enc(p));}
  if(!s.toLowerCase(Locale.ROOT).endsWith(".gguf"))throw new IllegalArgumentException("Requires .gguf");
  return b.toString();
 }
 public static String revision(String s) {
  if(s==null||s.isBlank()||s.length()>120||s.charAt(0)=='/')throw new IllegalArgumentException("Bad revision");
  StringBuilder b=new StringBuilder();
  for(String p:s.split("/",-1)){if(p.isEmpty()||p.equals(".")||p.equals("..")||!p.matches("[a-zA-Z0-9._-]{1,100}"))throw new IllegalArgumentException("Bad revision");
    if(b.length()>0)b.append('/');b.append(enc(p));}
  return b.toString();
 }
 public static String resolve(String repo,String rev,String file){return "https://huggingface.co/"+repo(repo)+"/resolve/"+revision(rev)+"/"+path(file);}
 public static String api(String repo){return "https://huggingface.co/api/models/"+repo(repo);}
 public static boolean trustedRedirect(String u){
  try{URI uri=URI.create(u);if(!"https".equalsIgnoreCase(uri.getScheme())||uri.getUserInfo()!=null||uri.getPort()!=-1||uri.getHost()==null)return false;
    String h=uri.getHost().toLowerCase(Locale.ROOT);
    return h.equals("huggingface.co")||h.endsWith(".huggingface.co")||h.equals("cdn-lfs.hf.co")||h.endsWith(".cdn-lfs.hf.co")||h.equals("cas-bridge.xethub.hf.co")||h.endsWith(".xethub.hf.co");
  }catch(RuntimeException e){return false;}
 }
}
