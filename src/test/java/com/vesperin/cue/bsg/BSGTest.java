package com.vesperin.cue.bsg;

import com.google.common.base.Joiner;
import com.google.common.collect.ImmutableList;
import com.vesperin.base.Context;
import com.vesperin.base.EclipseJavaParser;
import com.vesperin.base.Source;
import com.vesperin.base.locations.Locations;
import com.vesperin.base.locators.UnitLocation;
import com.vesperin.cue.bsg.visitors.BlockSegmentationVisitor;
import org.eclipse.jdt.core.dom.MethodDeclaration;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.util.List;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertThat;

/**
 * @author Huascar Sanchez
 */
public class BSGTest {
  private static final Predicate<UnitLocation> ONLY_MAIN = (u -> ((MethodDeclaration)u.getUnitNode())
    .getName().getIdentifier().equals("main"));

  private static final String TRY_CATCH = Joiner.on("\n").join(
    ImmutableList.of(
      "class TryCatch {"
      ,"   public static void main(String args[]) {"
      ,"     int num1, num2;"
      ,"     "
      ,"	   try { "
      ,"       num1 = 0;"
      ,"       num2 = 62 / num1;"
      ,"       println(\"Try block message\");"
      ,"     } catch (ArithmeticException e) { "
      ,"       println(\"Error: Don't divide a number by zero\");"
      ,"     }"
      ,"     "
      ,"	   println(\"I'm out of try-catch block in Java.\");"
      ,"   }"
      ,"   "
      ,"   private static void println(String message){"
      ,"     System.out.println(message);"
      ,"   }"
      ,"}"
    )
  );

  private Context context;

  @Before public void setUp() throws Exception {
    context = new EclipseJavaParser().parseJava(Source.from("TryCatch", TRY_CATCH));
  }

  @Test public void testBSGFromMethod() throws Exception {

    final List<UnitLocation> locatedUnitList = context.locateMethods().stream()
      .filter(ONLY_MAIN)
      .collect(Collectors.toList());

    final UnitLocation locatedUnit = locatedUnitList.get(0);
    assertNotNull(locatedUnit);

    final BlockSegmentationVisitor blockSegmentation = new BlockSegmentationVisitor(locatedUnit);
    locatedUnit.getUnitNode().accept(blockSegmentation);

    // the method declaration should have zero..or least two
    final SegmentationGraph graph = blockSegmentation.getBSG();
    assertThat(graph.getVertices().size() == 6, is(true));
    assertThat(graph.getEdges().size() == 4, is(true));

  }

  @Test public void testBSGFromClass() throws Exception {
    final List<UnitLocation> locatedUnitList = context.locateUnit(Locations.locate(context.getCompilationUnit()));
    final UnitLocation locatedUnit = locatedUnitList.get(0);
    assertNotNull(locatedUnit);

    final BlockSegmentationVisitor blockSegmentation = new BlockSegmentationVisitor(locatedUnit);
    locatedUnit.getUnitNode().accept(blockSegmentation);

    final SegmentationGraph graph = blockSegmentation.getBSG();
    assertThat(graph.getVertices().size() == 7, is(true));
    assertThat(graph.getEdges().size() == 7, is(true));

  }

  @After public void tearDown() throws Exception {
    context = null;
  }
}
