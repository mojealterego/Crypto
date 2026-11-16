import pl.omega.swarm.CommandCodec;
import java.util.Arrays;
public class CommandCodecTest {
 static void check(boolean x){if(!x)throw new AssertionError();}
 public static void main(String[] args)throws Exception{
  String n="123e4567-e89b-12d3-a456-426614174000";
  byte[] b=CommandCodec.canonical("asic-01",123,n,"STOP",0,0,"");
  check(Arrays.equals(b,CommandCodec.canonical("asic-01",123,n,"STOP",0,0,"")));
  check(b[0]=='O'&&b[5]==1);
  boolean fails=false;try{CommandCodec.canonical("../bad",123,n,"STOP",0,0,"");}catch(IllegalArgumentException e){fails=true;}check(fails);
  fails=false;try{CommandCodec.canonical("asic-01",123,n,"DVFS",Double.NaN,100,"");}catch(IllegalArgumentException e){fails=true;}check(fails);
  fails=false;try{CommandCodec.canonical("asic-01",123,n,"UNKNOWN",0,0,"");}catch(IllegalArgumentException e){fails=true;}check(fails);
  System.out.println("PASS");
 }
}
