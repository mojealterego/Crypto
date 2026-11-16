package pl.omega.swarm;
import org.json.*;import java.io.*;import java.net.*;import java.nio.charset.StandardCharsets;import java.util.*;
/** Hugging Face model metadata, size capped at 4MiB. */
public final class HuggingFaceCatalog {
 public static final class Catalog {
  public final String revisionSha; public final List<String> ggufFiles;
  Catalog(String sha,List<String> files){revisionSha=sha;ggufFiles=files;}
 }
 public static Catalog get(String repo,String token)throws Exception {
  HttpURLConnection c=(HttpURLConnection)URI.create(HuggingFacePaths.api(repo)).toURL().openConnection();
  c.setInstanceFollowRedirects(false);c.setConnectTimeout(15000);c.setReadTimeout(20000);
  c.setRequestProperty("Accept","application/json");
  if(token!=null&&!token.isBlank())c.setRequestProperty("Authorization","Bearer "+token.trim());
  try{
   int status=c.getResponseCode();if(status!=200)throw new IOException("HF API HTTP "+status);
   if(c.getContentLength()>4*1024*1024)throw new IOException("HF index too big");
   ByteArrayOutputStream output=new ByteArrayOutputStream();
   try(InputStream in=c.getInputStream()){byte[] b=new byte[8192];int n;
    while((n=in.read(b))!=-1){if(output.size()+n>4*1024*1024)throw new IOException("HF index too big");output.write(b,0,n);}}
   JSONObject json=new JSONObject(output.toString(StandardCharsets.UTF_8));
   JSONArray siblings=json.optJSONArray("siblings");List<String> files=new ArrayList<>();
   if(siblings!=null)for(int i=0;i<siblings.length();i++){
    JSONObject entry=siblings.optJSONObject(i);String file=entry==null?"":entry.optString("rfilename","");
    if(file.toLowerCase(Locale.ROOT).endsWith(".gguf")){try{HuggingFacePaths.path(file);files.add(file);}catch(IllegalArgumentException ignored){}}
   }
   Collections.sort(files);String sha=json.optString("sha","");
   if(!sha.matches("(?i)[0-9a-f]{40}"))sha="main";
   return new Catalog(sha,files);
  }finally{c.disconnect();}
 }
}
