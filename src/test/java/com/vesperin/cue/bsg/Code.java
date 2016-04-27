package com.vesperin.cue.bsg;

import com.google.common.base.Joiner;
import com.google.common.collect.ImmutableList;
import com.vesperin.base.Source;

/**
 * @author Huascar Sanchez
 */
class Code {
  static final Source TRY_CATCH = Source.from("TryCatch", Joiner.on("\n").join(
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
  ));

  static final Source WHILE_LOOP = Source.from("WhileLoop", Joiner.on("\n").join(
    ImmutableList.of(
      "class WhileLoop {"
      ,"   // Given a num, returns how many times can we divide it by 2 to get down to 1."
      ,"   int count2Div(int num) {"
      ,"     int count = 0;   // count how many divisions we've done"
      ,"     while (num >= 1) {"
      ,"       num = num / 2;"
      ,"       count++;"
      ,"     }"
      ,"     return count;"
      ,"   }"
      ,"}"
    )
  ));
}
