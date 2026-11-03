package vn.chamanh;
import java.util.Arrays;
import java.util.Random;

public final class ImageMatcherTest {
    static int tests=0;
    static void check(boolean condition,String name){tests++;if(!condition)throw new AssertionError(name);}
    static int[] flat(int w,int h,int color){int[] a=new int[w*h];Arrays.fill(a,color);return a;}
    static int[] pattern(int w,int h,long seed){Random r=new Random(seed);int[] a=new int[w*h];for(int y=0;y<h;y++)for(int x=0;x<w;x++){int red=(x*19+y*7+r.nextInt(55))&255,green=(x*3+y*23+r.nextInt(35))&255,blue=(x*13+y*11+r.nextInt(40))&255;a[y*w+x]=0xff000000|(red<<16)|(green<<8)|blue;}return a;}
    static void insert(int[] screen,int sw,int[] t,int tw,int th,int x,int y,int noise){Random r=new Random(42);for(int j=0;j<th;j++)for(int i=0;i<tw;i++){int p=t[j*tw+i];if(noise>0){int a=Math.min(255,Math.max(0,((p>>>16)&255)+r.nextInt(2*noise+1)-noise)),b=Math.min(255,Math.max(0,((p>>>8)&255)+r.nextInt(2*noise+1)-noise)),c=Math.min(255,Math.max(0,(p&255)+r.nextInt(2*noise+1)-noise));p=0xff000000|(a<<16)|(b<<8)|c;}screen[(y+j)*sw+x+i]=p;}}
    static void position(ImageMatcher.Result r,int x,int y,String name){check(r!=null,name+" found");check(r.centerX==x&&r.centerY==y,name+" center "+(r==null?"null":r.centerX+","+r.centerY));}
    public static void main(String[] args){
        int[] t=pattern(28,22,7),screen=flat(200,160,0xffe9ede5);insert(screen,200,t,28,22,91,57,4);
        ImageMatcher.Result r=ImageMatcher.find(screen,200,160,t,28,22,.90);position(r,105,68,"Noisy known template");check(r.score>.97,"Noisy score meaningful");
        check(ImageMatcher.find(flat(200,160,0xffeeeedd),200,160,t,28,22,.90)==null,"Flat screen absent");
        int[] other=pattern(200,160,192);check(ImageMatcher.find(other,200,160,t,28,22,.92)==null,"Unrelated textured screen absent");
        int[] uniform=flat(20,20,0xff123456);check(!ImageMatcher.hasUsableDetail(uniform,20,20),"Flat crop rejected");check(ImageMatcher.find(screen,200,160,uniform,20,20,.80)==null,"Flat template no result");
        int[] darkNoise=flat(20,20,0xff434343);for(int i=0;i<darkNoise.length;i++)darkNoise[i]+=(i%3)*0x010101;check(!ImageMatcher.hasUsableDetail(darkNoise,20,20),"Near flat rejected");
        screen=flat(47,43,0xff001b2a);insert(screen,47,t,28,22,19,21,0);position(ImageMatcher.find(screen,47,43,t,28,22,.99),33,32,"Bottom right boundary");
        screen=flat(47,43,0xff001b2a);insert(screen,47,t,28,22,0,0,0);position(ImageMatcher.find(screen,47,43,t,28,22,.99),14,11,"Top left boundary");
        position(ImageMatcher.find(t,28,22,t,28,22,1.0),14,11,"Whole screen equal");
        check(ImageMatcher.find(t,28,22,pattern(29,22,1),29,22,.8)==null,"Oversize template rejected");
        int[] tiny=pattern(4,4,14);screen=flat(20,20,0xff909090);insert(screen,20,tiny,4,4,7,11,0);position(ImageMatcher.find(screen,20,20,tiny,4,4,.99),9,13,"Small image");
        // Coarse downsampling + native refinement, deliberately off coarse grid.
        t=pattern(54,42,9);screen=flat(1200,900,0xffcddbd4);insert(screen,1200,t,54,42,733,417,3);long start=System.nanoTime();r=ImageMatcher.find(screen,1200,900,t,54,42,.94);position(r,760,438,"Downsample native refinement");System.out.println("1200x900 match: "+((System.nanoTime()-start)/1000000)+"ms");
        // Color similarity alone is insufficient: same palette, unrelated order.
        int[] shuffled=t.clone();Random rng=new Random(313);for(int i=shuffled.length-1;i>0;i--){int j=rng.nextInt(i+1),v=shuffled[i];shuffled[i]=shuffled[j];shuffled[j]=v;}screen=flat(150,120,0xffeeeeee);insert(screen,150,shuffled,54,42,33,40,0);check(ImageMatcher.find(screen,150,120,t,54,42,.90)==null,"Same palette wrong structure rejected");
        // A matching transparent border is ignored while an opaque distinctive center is found.
        int[] transparent=pattern(24,24,55);for(int y=0;y<24;y++)for(int x=0;x<24;x++)if(x<4||y<4||x>19||y>19)transparent[y*24+x]=0;
        screen=flat(80,80,0xff778899);for(int y=4;y<20;y++)for(int x=4;x<20;x++)screen[(y+27)*80+x+19]=transparent[y*24+x];
        position(ImageMatcher.find(screen,80,80,transparent,24,24,.99),31,39,"Transparent border mask");
        boolean error=false;try{ImageMatcher.find(screen,80,80,t,54,42,1.1);}catch(IllegalArgumentException e){error=true;}check(error,"Invalid threshold rejected");
        error=false;try{ImageMatcher.find(new int[2],150,120,t,54,42,.90);}catch(IllegalArgumentException e){error=true;}check(error,"Invalid pixels rejected");
        Thread.currentThread().interrupt();check(ImageMatcher.find(screen,80,80,t,54,42,.9)==null,"Interrupted returns null");check(Thread.interrupted(),"Interrupt flag preserved");
        System.out.println("PASS "+tests+" checks");
    }
}
