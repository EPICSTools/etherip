package org.epics.etherip.types;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 12/14/16
 *
 * @author MxBlinkPx
 */
public class CNSymbolMultiPath extends CNPath  {

    class PathAndIndex{
        private String path;
        private Integer index;
        public PathAndIndex(String path, Integer index) {
            this.path = path;
            this.index = index;
        }
        public String getPath() {
            return path;
        }
        public Integer getIndex() {
            return index;
        }

    };

    private final Pattern PATTERN_BRACKETS = Pattern.compile("\\[(\\d+)\\]");

    private List<PathAndIndex> paths = new ArrayList<>();

    /** Initialize
     *  @param symbol Name of symbol
     */
    protected CNSymbolMultiPath(final String symbol)
    {
        for(String s:symbol.split("\\.")){
            Matcher m = PATTERN_BRACKETS.matcher(s);
            Integer index = null;
            String path = s;
            while(m.find()){
                String match = m.group().replace("[","").replace("]", "");
                index = Integer.parseInt(match);
                path = path.replace("[" + match + "]", "");
            }
            paths.add(new PathAndIndex(path, index));
        }
    }

    /** {@inheritDoc} */
    @Override
    public int getRequestSize()
    {   // End of string is padded if length is odd
        int count = 0;
        for(PathAndIndex s:paths){
            count += 2 + s.getPath().length() + (needPad(s.getPath()) ? 1 : 0);
            if(s.getIndex()!=null)
                count += 2;
        }
        return count;
    }

    /** {@inheritDoc} */
    @Override
    public void encode(final ByteBuffer buf, final StringBuilder log)
    {
        // spec 4 p.21: "ANSI extended symbol segment"
        buf.put((byte) (getRequestSize() / 2));
        for(PathAndIndex pi:paths){
            String s = pi.getPath();
            buf.put((byte) 0x91);
            buf.put((byte) s.length());
            buf.put(s.getBytes());
            if (needPad(s))
                buf.put((byte) 0);
            Integer index = pi.getIndex();
            if(index!=null){
                //Path Segment 28, from wireshark
                buf.put((byte) 0x28);
                buf.put(index.byteValue());
            }
        }

    }

    /** @return Is path of odd length, requiring a pad byte? */
    private boolean needPad(String s)
    {
        // Findbugs: x%2==1 fails for negative numbers
        return (s.length() % 2) != 0;
    }
}

