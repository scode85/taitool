package vn.chamanh;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/** Same-size, on-device RGB template matching. No Android or network dependencies. */
public final class ImageMatcher {
    private static final int COARSE_LIMIT = 600;
    private static final int COARSE_CANDIDATES = 24;
    private static final int VERIFY_CANDIDATES = 8;
    private static final double MIN_DEVIATION = 7.0;

    private ImageMatcher() {}

    public static final class Result {
        public final int centerX;
        public final int centerY;
        /** Minimum of RGB agreement and spatial correlation; 0..1. */
        public final double score;
        public Result(int centerX, int centerY, double score) {
            this.centerX = centerX;
            this.centerY = centerY;
            this.score = score;
        }
    }

    /**
     * Rejects tiny, transparent, or almost flat crops. Such crops can match many
     * unrelated screen regions. Choose a distinctive icon/text/button instead.
     */
    public static boolean hasUsableDetail(int[] pixels, int width, int height) {
        validate(pixels, width, height);
        if (width < 4 || height < 4) return false;
        Samples s = samples(pixels, width, height, 20);
        return s.n >= 12 && s.deviation() >= MIN_DEVIATION;
    }

    /**
     * Finds a same-scale template. The template must be cropped from a screenshot
     * at the current device resolution/orientation; this does not resize controls
     * or perform semantic/OCR matching. Returns null when absent, unreliable, too
     * large, or interrupted. Invalid dimensions/threshold throw IllegalArgumentException.
     */
    public static Result find(int[] screen, int sw, int sh,
                              int[] template, int tw, int th, double threshold) {
        validate(screen, sw, sh);
        validate(template, tw, th);
        if (Double.isNaN(threshold) || threshold < 0.0 || threshold > 1.0)
            throw new IllegalArgumentException("Threshold must be between 0 and 1");
        if (tw > sw || th > sh || !hasUsableDetail(template, tw, th)
                || Thread.currentThread().isInterrupted()) return null;

        int factor = Math.max(1, (Math.max(sw, sh) + COARSE_LIMIT - 1) / COARSE_LIMIT);
        // Retain at least four coarse pixels on the short template dimension.
        factor = Math.min(factor, Math.max(1, Math.min(tw, th) / 4));
        int cw = sw / factor, ch = sh / factor;
        int ctw = tw / factor, cth = th / factor;
        int[] coarseScreen = shrink(screen, sw, sh, factor);
        int[] coarseTemplate = shrink(template, tw, th, factor);
        Samples coarse = samples(coarseTemplate, ctw, cth, 10);
        if (coarse.n < 4 || coarse.variance < 0.00001) return null;
        List<Candidate> seeds = new ArrayList<Candidate>();
        double floor = Math.max(0.10, threshold - 0.65);
        for (int y = 0; y <= ch - cth; y++) {
            if ((y & 31) == 0 && Thread.currentThread().isInterrupted()) return null;
            for (int x = 0; x <= cw - ctw; x++) {
                double score = score(coarseScreen, cw, x, y, coarse);
                if (score >= floor) offer(seeds, new Candidate(x * factor, y * factor, score), COARSE_CANDIDATES);
            }
        }
        if (seeds.isEmpty()) return null;
        Samples fine = samples(template, tw, th, 16);
        List<Candidate> finalists = new ArrayList<Candidate>();
        Set<Long> visited = new HashSet<Long>();
        int radius = factor + 1;
        for (Candidate seed : seeds) {
            if (Thread.currentThread().isInterrupted()) return null;
            int minX = Math.max(0, seed.x - radius), maxX = Math.min(sw - tw, seed.x + radius);
            int minY = Math.max(0, seed.y - radius), maxY = Math.min(sh - th, seed.y + radius);
            for (int y = minY; y <= maxY; y++) for (int x = minX; x <= maxX; x++) {
                long key = ((long)y << 32) | (x & 0xffffffffL);
                if (!visited.add(key)) continue;
                double value = score(screen, sw, x, y, fine);
                if (value + 0.04 >= threshold)
                    offer(finalists, new Candidate(x, y, value), VERIFY_CANDIDATES);
            }
        }
        Result best = null;
        // Final verification covers every non-transparent pixel. Sparse anchors
        // alone can otherwise accept a similar-looking but different image.
        for (Candidate c : finalists) {
            if (Thread.currentThread().isInterrupted()) return null;
            double exact = denseScore(screen, sw, c.x, c.y, template, tw, th);
            if (exact + 1e-9 >= threshold && (best == null || exact > best.score))
                best = new Result(c.x + tw / 2, c.y + th / 2, exact);
        }
        return best;
    }

    private static void validate(int[] pixels, int w, int h) {
        if (pixels == null || w <= 0 || h <= 0 || (long) w * h > pixels.length)
            throw new IllegalArgumentException("Invalid image dimensions/pixel array");
    }

    private static final class Candidate {
        final int x, y;
        final double score;
        Candidate(int x, int y, double score) { this.x=x; this.y=y; this.score=score; }
    }
    private static void offer(List<Candidate> list, Candidate candidate, int limit) {
        if (list.size() >= limit && candidate.score <= list.get(list.size()-1).score) return;
        int i=0;
        while (i < list.size() && list.get(i).score >= candidate.score) i++;
        list.add(i, candidate);
        if (list.size() > limit) list.remove(list.size()-1);
    }

    private static int[] shrink(int[] source, int w, int h, int factor) {
        if (factor == 1) return source;
        int dw=w/factor, dh=h/factor;
        int[] out = new int[dw*dh];
        for (int y=0;y<dh;y++) for(int x=0;x<dw;x++) {
            long r=0,g=0,b=0,a=0;
            for(int ky=0;ky<factor;ky++) for(int kx=0;kx<factor;kx++) {
                int p=source[(y*factor+ky)*w+x*factor+kx];
                a+=(p>>>24);r+=(p>>>16)&255;g+=(p>>>8)&255;b+=p&255;
            }
            int n=factor*factor;
            out[y*dw+x]=((int)(a/n)<<24)|((int)(r/n)<<16)|((int)(g/n)<<8)|(int)(b/n);
        }
        return out;
    }

    private static final class Samples {
        final int[] x, y, r, g, b;
        int n;
        double sr,sg,sb,squares,variance;
        Samples(int capacity) { x=new int[capacity];y=new int[capacity];r=new int[capacity];g=new int[capacity];b=new int[capacity]; }
        void add(int px,int py,int color) {
            if ((color>>>24) < 32) return;
            x[n]=px;y[n]=py;r[n]=(color>>>16)&255;g[n]=(color>>>8)&255;b[n]=color&255;
            sr+=r[n];sg+=g[n];sb+=b[n];squares+=(double)r[n]*r[n]+(double)g[n]*g[n]+(double)b[n]*b[n];n++;
        }
        void finish(){variance=n==0?0:Math.max(0,squares-(sr*sr+sg*sg+sb*sb)/n);}
        double deviation(){return n==0?0:Math.sqrt(variance/(3*n));}
    }
    private static Samples samples(int[] template,int w,int h,int grid) {
        int gx=Math.min(grid,w),gy=Math.min(grid,h);
        Samples s=new Samples(gx*gy);
        for(int y=0;y<gy;y++) for(int x=0;x<gx;x++) {
            int px=gx==1?0:x*(w-1)/(gx-1),py=gy==1?0:y*(h-1)/(gy-1);
            s.add(px,py,template[py*w+px]);
        }
        s.finish();return s;
    }
    private static double score(int[] screen,int sw,int ox,int oy,Samples t) {
        if(t.n==0 || t.variance<0.00001)return 0;
        double sr=0,sg=0,sb=0,squares=0,cross=0,absolute=0;
        for(int i=0;i<t.n;i++) {
            int p=screen[(oy+t.y[i])*sw+ox+t.x[i]];
            int r=(p>>>16)&255,g=(p>>>8)&255,b=p&255;
            sr+=r;sg+=g;sb+=b;squares+=(double)r*r+(double)g*g+(double)b*b;
            cross+=(double)r*t.r[i]+(double)g*t.g[i]+(double)b*t.b[i];
            absolute+=Math.abs(r-t.r[i])+Math.abs(g-t.g[i])+Math.abs(b-t.b[i]);
        }
        double variance=Math.max(0,squares-(sr*sr+sg*sg+sb*sb)/t.n);
        if(variance<0.00001)return 0;
        double covariance=cross-(sr*t.sr+sg*t.sg+sb*t.sb)/t.n;
        double corr=Math.max(0,Math.min(1,covariance/Math.sqrt(variance*t.variance)));
        double color=1.0-absolute/(t.n*3.0*255.0);
        return Math.min(color,corr);
    }
    private static double denseScore(int[] screen,int sw,int ox,int oy,int[] template,int tw,int th) {
        double tr=0,tg=0,tb=0,sr=0,sg=0,sb=0,tq=0,sq=0,cross=0,absolute=0;
        int n=0;
        for(int y=0;y<th;y++) {
            if((y&63)==0 && Thread.currentThread().isInterrupted())return 0;
            for(int x=0;x<tw;x++) {
                int t=template[y*tw+x];if((t>>>24)<32)continue;
                int s=screen[(oy+y)*sw+ox+x];
                int a=(t>>>16)&255,b=(t>>>8)&255,c=t&255,r=(s>>>16)&255,g=(s>>>8)&255,v=s&255;
                tr+=a;tg+=b;tb+=c;sr+=r;sg+=g;sb+=v;
                tq+=(double)a*a+(double)b*b+(double)c*c;sq+=(double)r*r+(double)g*g+(double)v*v;
                cross+=(double)a*r+(double)b*g+(double)c*v;
                absolute+=Math.abs(a-r)+Math.abs(b-g)+Math.abs(c-v);n++;
            }
        }
        if(n==0)return 0;
        double tv=Math.max(0,tq-(tr*tr+tg*tg+tb*tb)/n),sv=Math.max(0,sq-(sr*sr+sg*sg+sb*sb)/n);
        if(tv<MIN_DEVIATION*MIN_DEVIATION*3*n || sv<0.00001)return 0;
        double covariance=cross-(tr*sr+tg*sg+tb*sb)/n;
        double correlation=Math.max(0,Math.min(1,covariance/Math.sqrt(tv*sv)));
        double color=1-absolute/(n*3.0*255.0);
        return Math.min(color,correlation);
    }
}
