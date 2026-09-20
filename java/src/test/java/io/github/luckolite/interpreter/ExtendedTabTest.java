// Copyright 2026 Luckolite
// SPDX-License-Identifier: Apache-2.0
package io.github.luckolite.interpreter;
import org.junit.Test;import java.util.*;import static org.junit.Assert.*;
/** Original synthetic guitar geometry; no score-derived fixtures. */
public class ExtendedTabTest {
 @Test public void standardAndSevenStringHeaders(){assertEquals(List.of(64,59,55,50,45,40),TabTuning.parse("Tuning : E A D G B E",6));assertEquals(List.of(64,59,55,50,45,40,35),TabTuning.parse("Tuning: B E A D G B E",7));}
 @Test public void alternateTuningsAndFlatOctaves(){assertEquals(List.of(62,57,55,50,45,38),TabTuning.parse("Tuning : D A D G A D",6));assertEquals(List.of(59,54,50,45,40,33),TabTuning.parse("Tuning : A E A D G♭ B",6));assertEquals(List.of(64,59,55,50,45,40),TabTuning.parse("Tuning: E2 A2 D3 G3 B3 E4",6));}
 @Test public void malformedOrWrongCountIsNotGuessed(){for(String s:List.of("E A D G B E","Tuning: E A D G B","Tuning: E A D G B E E","Tuning: E2 A2 D3 G3 B3 E3"))assertTrue(s,TabTuning.parse(s,6).isEmpty());}
 @Test public void seventhStringPitchAndExplicitTuning(){assertEquals(35,TablatureDecoder.midi(6,0,new int[]{64,59,55,50,45,40,35},0));assertEquals(49,TablatureDecoder.midi(6,12,new int[]{64,59,55,50,45,40,35},2));}
 byte[] page(int width,int end,int strings){byte[] g=new byte[width*320];Arrays.fill(g,(byte)255);for(int s=0;s<strings;s++)for(int x=20;x<=end;x++)g[(50+s*20)*width+x]=0;for(int x:new int[]{20,end})for(int y=50;y<=50+(strings-1)*20;y++)g[y*width+x]=0;return g;}
 @Test public void shortFinalSystemHasAllStrings(){var t=TablatureDecoder.detect(page(1000,150,6),1000,320);assertEquals(1,t.size());assertEquals(6,t.get(0).stringCount());assertEquals(2,t.get(0).bars().size());}
 @Test public void denseFretsDoNotInvalidateMatchingRuleExtents(){byte[] g=page(600,570,7);for(int x=100;x<340;x++)g[70*600+x]=(byte)255;var rows=TablatureDecoder.detect(g,600,320);assertEquals(1,rows.size());assertEquals(7,rows.get(0).stringCount());}
 @Test public void sevenStringMaskIncludesLowestString(){byte[] g=page(600,570,7);var rows=TablatureDecoder.detect(g,600,320);assertEquals(255,TablatureDecoder.withoutTabs(g,600,320,rows,true)[170*600+100]&255);}
 @Test public void seventhStringFretSurvivesWordAndRhythmTransforms(){var t=new TablatureDecoder.Staff(50,20,-1,List.of(),List.of(20f,570f),7,List.of());var rows=TablatureDecoder.withWords(List.of(t),List.of(new TablatureDecoder.Word("12",.15f,.51f,.18f,.55f)),600,320);assertEquals(6,rows.get(0).frets().get(0).string());assertEquals(7,rows.get(0).stringCount());}
 @Test public void shortHalfStemIsNotAnEighthFromNearbyText(){byte[] g=page(600,570,6);for(int y=183;y<204;y++)g[y*600+100]=0;for(int y=190;y<220;y++)g[y*600+94]=0;for(int x=80;x<115;x++)g[218*600+x]=0;var t=new TablatureDecoder.Staff(50,20,-1,List.of(new TablatureDecoder.Fret(100,50,0,8)),List.of(20f,570f));var f=TabNotation.rasterRhythm(List.of(t),g,600,320).get(0).frets().get(0);assertEquals(2,f.duration(),0);assertEquals(0,f.beams());}
 @Test public void beamEndingToLeftStillMakesSixteenth(){byte[] g=page(600,570,6);for(int y=162;y<=202;y++)g[y*600+200]=0;for(int y:new int[]{188,201})for(int x=160;x<=200;x++)g[y*600+x]=0;var t=new TablatureDecoder.Staff(50,20,-1,List.of(new TablatureDecoder.Fret(200,50,0,8)),List.of(20f,570f));assertEquals(2,TabNotation.rasterRhythm(List.of(t),g,600,320).get(0).frets().get(0).beams());}
 @Test public void mutedAttackRetainsRhythm(){byte[] g=page(600,570,6);for(int y=162;y<=202;y++)g[y*600+200]=0;for(int x=200;x<230;x++)g[200*600+x]=0;var t=new TablatureDecoder.Staff(50,20,-1,List.of(new TablatureDecoder.Fret(200,50,0,-1)),List.of(20f,570f));assertEquals(1,TabNotation.rasterRhythm(List.of(t),g,600,320).get(0).frets().get(0).beams());}
}
