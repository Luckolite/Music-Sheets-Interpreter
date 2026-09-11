// Copyright 2026 Luckolite
// SPDX-License-Identifier: Apache-2.0
package io.github.luckolite.interpreter;
import java.util.*;
import java.io.*;
import java.util.zip.GZIPInputStream;
import org.junit.Test;
import static org.junit.Assert.*;

/** Original rendered words at a held-out size and suffix ratio, plus bare-number controls. */
public class PrintedOctaveWordTest {
    private record Glyph(int width,int height,String encoded) {
        byte[] pixels()throws Exception {try(var in=new GZIPInputStream(new ByteArrayInputStream(Base64.getDecoder().decode(encoded)))){return in.readAllBytes();}}
    }
    static final Glyph G0=new Glyph(37,19,"H4sIAAAAAAAC/32STUiUURSGnxlnvhmEkVDLcBYV5Q/EVFQGbiSslS3KMiiEICqJMBICV0KMMG4UISFRcBGBQhLRRqhoEUITDUFSFkmtBn+YTVEUiM58b+fOtJ25m/eecx4O7z33SHYWz+4KNVxcUqUzHqDvVwrvuQrvR7uafGkilnSF3LUdTZ+qOGjXtwG8H9oK0ZhfCAIfpDiRbel7PDifPg4DBl2BAya1sKRcI0xLLQS3tbqHW1IUFqxqbLNJCL5IA9AvzXNMOoe3rkIV3h+rdkBwQ1nYX5BmoMty1TN6BZelz3DS2RsyH9c1TeS1BS9xTZaP5NUJT6VRSDkoGzFqaGci7YKPsFf+ha9aCxD9K7VCpjiClEEMl8axDrs1+FiahYT0BGoLLp/psYdRv1qENqHu3qRdhp25323QY8HWzUD3z6NGdZZahQk9cmpmErnzN2AiM+t3E15TtsaoF0WouqakaedhqhdOXPUfwhlLJS0zWCw2L///rDuxljklo/tG8m5IY5Z5Z9Dtsp8bL019xaD7ZaFDsGjyDLxsWWgE5kxOQbL8Lm2e5vC3jT6461fYuPyD9lg4fulNpa38B53Toji/AgAA"); // 8va
    static final Glyph G1=new Glyph(37,19,"H4sIAAAAAAAC/32RX4iMYRSHn5nm+2ZaZi/Y9WemDbErahEuSCT2Qkuxodyg0LZlN6RWakuzWhdITHa31V7IBXEhN1NGLrSFVlO0bSKUJnajkBtNuzPf8fu+2bmYLc7N+Z3zPu95zzmvmWxk74LIwoNvbJZN3OtY1VIJroVo/92H+7ia8WLA9ZngZQj3p01FSBSrqdIS+DSjj8AKuXkw68HPsLqiN0KTXATeVkMDcK6it0F40vKwvFTKXWlt9MzS8ZQO9kD2+NzELR/qUX8nbIjoM8uEpV+bJYlOW2EO8a2KQ2OC8lGpnvrmF9LfEjBktpLwtGUh9vC7mjnjl+oTRG/57dPQafaADWandNXsMuxT/tUBDUbdlwAahla5mmGzRnhn1guHbaoj1PZrvagdAfQEv8j4uqJ9hGVKHIWLXhvOV8vXisr60BgsNW+/SqThrBINkLsNuyVTgrqD/4JF1n1fahdolKewxV/SVWVGBXX5UAHmXxiQ+BMjqY1txxnVRsgo9V7QjaAph8gd32eCW4OQNlsDI0o9AjcfQDW1QW92EgZ/9DvuTelLcFduJ6TKi2oaL/vc+U2L3YZjwXcWWlj7YbJdk3j2byv2b447yUPP/4PYX2ugSQW/AgAA"); // 8vb
    static final Glyph G2=new Glyph(60,19,"H4sIAAAAAAAC/41SS0iUURT+ZqYcY7JGix5IDyl76VCkWZRkShATLaIsk2gRldALe7gRWmj0WBgtTCgoSiktTCqlhChsESoKLXrBLLQ0tBoipWmMwJn5Ovf+d5pZ/t/m/86933fOuec/pEakt3ZjCQ0ms6BxhbF3DXvn1zBQNP0qOVwyo0nfBw95s9+7kKP4l1t70kVaF/fesawY4GP16Q3MAuZyOBNYqq4HMp2tPfnAKRWszkxVmoCxRpZZ1lzhXUDGeIUkc/+puQl45WhkEY6R4nhmySfSgOx42eYd/I/nQFn1j2qg8Hq4CsiXo51I+cqoCylhSzIoZc4YeWxVa8Ir+gNNLAB2dzMPqCJfAuXkR2CLkVwQb5fhD4EFZ4dMsBLw85cLOMFxJ/CKLAEekXXARSORlN5Jw9eot3oeaP5J6Au2y3NH1NxEM+pA6gS5Aui35EEHsM9Y261BTXmtgnogj6yUxsmT8nQZBuAj22SEUUvfKOLm+M8dalmnzD4VbAPukrlAB5mj+XlgO0OiKDX6MsA1lhjQ5HFllp7Cbjh/Mgg10zHp7dvpiDzUF9x1BKjv19UiGcBmJiFaLN5G8gmwgbyvZ9on+f197FFpb+wHCg7GlLQ7eak0lOIaKelryMN6pr8LPf4PclWZtryFtalZlyJaeS5pqQwWA09pB2uTlspgK6aF7Fi/OxJLFUcRjtoqext6YZIRm7dk3JZ3PZAeMfyyZ9Nn+dyb/daG8e9ghZR1d4Zi8YWc0xbqKB21UxRxNOjwTfHMqQvLO231y3+1jGe4dAQAAA=="); // 15ma
    static final Glyph G3=new Glyph(60,19,"H4sIAAAAAAAC/41TW0iTYRh+pu0Q00qLysYqKQPNKNJK0vKAUAMvgg7WRQQdpCgJU4iBF+tEUBFh1qCilDKjooOUIIVdhMrCLpKIXbia0clCR2uJsLW39/v+72e/MMTnYu/7vN/z7vvew08kEes7sb6CFKLZkLhASfGt/WBupU4+39iewdLzOr+lpWIwaWrcxkeXdLbSISj8isaWaan5ya+lf4uAQIL+TQdydNJWRZMiCOQZaICvOaa/Ke/+5LlXgeMGeppzu5X/AHDWB9X/DDTvyPKQvzTtItFQxYxWEawCuvalLbim9AXArKjyV4la7fek/1j4ff7ZwDwacgBLOTZuR/oGDpsGpGTYBOxUqU+1Rk17LVk3kBmq4cZbxzzX+QIOdQG2Rz8LgTqpaGFxmz7c4N01InmFZCysdv9yAyXeSANQyKGjQCPROWCLVFQDqaOJ6qOHRfIb4bJ+dyutBbb2iMIaOJQjp3mSD+RAM4GNEyZYzrktwssFXPQ7FThCoRTgFdEgkM0He4BTQtBjXCqJXo5cZvuR7QvRAusX0TfRzyagnk+cQL9QNhqWSmEx8IyksEBWyO+r5dI5tBnoJXoJFEvhasNSKVRiepjNJuA2UT7QQbRc+mM2OOJEZTD7hO6HKbFUOkpxiH8jVqSM0DBgidAoq77XxZ4DtUReoEnqbkI2YcK3Mn9JiM0ToIioHSgj8vEsXD7iEXhHrpgtzZpwHZARU0ln7cWf2NyZ806wA4CHaD9whuhPid31nqjfXZRlce79II7HAzV8rbUzHNcXcu7DcMe2rzQFQIf2iLflM80Ld3XSlPAfOrZjGXQEAAA="); // 15mb
    static final Glyph G4=new Glyph(29,19,"H4sIAAAAAAAC//v/Hwz+HG+0cvoPBb8VGcCgB8R5NCdUEMjuhknOh8gx3AFx9KU5QOwbULk/ahA5HSj/Ky8DgypM41Kf/yjgLlBhEZT9T2sVqmQLUHIflL2agUG2+AGSpDEDg8BvKNsAZB/3CrjcS0YGhggoeyPENSyHYZILgLylME8+WGYKktWFSYYzMDC/Q1jyOxskewrqMSEGBjtk9/11BEougLCPIgcPGBwDikyGMGuQggcKFBgYtkBYRkjBAwUuDJyfwIwXjIjggQF7hkwIYx7Q1P2ocv8klN9DWOYMDIJ/oKLt3Nb3gdQSkYsg3o+7aUCN7Ns+/YOFndiaT5tCnoJ5DDAwBcw968jPKhe5DWoOAPwcu9onAgAA"); // 15
    static final Glyph G5=new Glyph(29,19,"H4sIAAAAAAAC/02RP0hCURTGvyf6jP4NWglJNBgtYUFIFEFD2VQUQkE41BJSkBAVTS4GL5cIGoraGiKKICIKipYSTGxqCCHaHoqbIYSEvDyd98/nGe73HX6Xe+45h4gjOdth98y/U31UJnGs6r6ASEmC+FDHqmEIeda0ALFIFTs6FQuuA0OqLgI9LC7AejgBQFJNAOhlsQNZk50yw4fqxgBbgWTA92ewO74In2Zj7JbpBM5ng6Ub4Qc2NC87mcba/a8Gy7oQ3QJe9ExSK+yY9XJdiFe74da//jbHH0VbTmfFPtsRpYAlbRIrQuh7kOm4xsqj4gXRGnCtTiIER57kVqaPnCozTSyKBw0/ekdTLHGG26yr7gyfT8C00eQeS4ZhlKjkQC2CRF7gnuEnZwdElxbDGVE/kGR4A4iy2cwtMKyZXeCcZQKI16YeBhKa+Q1i4KsQATarJis311agHI60OLwLKWuVV/oKOf4BLM4QdScCAAA="); // 84
    private int match(Glyph g)throws Exception {return OctaveWordShapes.match(g.pixels(),g.width,0,0,g.width-1,g.height-1);}
    @Test public void recognizesUpperOctave()throws Exception {assertEquals(1,match(G0));}
    @Test public void recognizesLowerOctave()throws Exception {assertEquals(-1,match(G1));}
    @Test public void recognizesTwoUpperOctaves()throws Exception {assertEquals(2,match(G2));}
    @Test public void recognizesTwoLowerOctaves()throws Exception {assertEquals(-2,match(G3));}
    @Test public void bareNumbersDoNotBecomeOctaveDirections()throws Exception {assertEquals(0,match(G4));assertEquals(0,match(G5));}
    @Test public void automaticSpanNeedsNoCallerOcr()throws Exception {
        int w=500,h=260;byte[] gray=new byte[w*h];Arrays.fill(gray,(byte)255);byte[] glyph=G0.pixels();
        for(int y=0;y<G0.height;y++)System.arraycopy(glyph,y*G0.width,gray,(40+y)*w+70,G0.width);
        for(int x=70+G0.width+6;x<300;x++)if((x-70-G0.width-6)%14<7)gray[55*w+x]=gray[56*w+x]=0;
        var staff=new PlayingTechniqueDetector.Staff(100,164,16,0,1);
        var first=new ScoreNoteEvent(0,.4f,2,0,1,140f/h,false,0,0,2,1);
        var after=new ScoreNoteEvent(0,.8f,2,0,1,140f/h,false,0,0,2,1);
        var notes=OctaveMarkDetector.apply(List.of(),List.of(staff),List.of(new MeasureRegion(0,1,0,1)),List.of(first,after),gray,w,h);
        assertEquals(1,notes.get(0).octaveShift());assertEquals(0,notes.get(1).octaveShift());
    }
    @Test(expected=IllegalArgumentException.class) public void outOfRangeShiftIsRejected() {
        new ScoreNoteEvent(0,0,0,0,1,0,false,0,0,2,1,1,0,0,30,false,0,false,3);
    }
}
