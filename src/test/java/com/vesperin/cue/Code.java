package com.vesperin.cue;

import com.google.common.base.Joiner;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;
import com.vesperin.base.Source;

import java.util.List;

/**
 * @author Huascar Sanchez
 */
public class Code {
  private static final String ONE = Joiner.on("\n").join(
    ImmutableList.of(
    "public class Fibonacci {"
    , "    public static long fib(int n) {"
      ,"        if (n <= 1) return n;"
      ,"        else return fib(n-1) + fib(n-2);"
      ,"    }"
      ,""
      ,"    public static void main(String[] args) {"
      ,"        int N = Integer.parseInt(args[0]);"
      ,"        for (int i = 1; i <= N; i++)"
      ,"            StdOut.println(i + \": \" + fib(i));"
      ,"    }"
      ,""
      ,"}"
    )
  );

  private static final String TWO = Joiner.on("\n").join(
    ImmutableList.of(
    "public class Fibonacci {"
    ,"	public static int fib(int n) {"
      ,"        if (n < 2) {"
      ,"           return n;"
      ,"        } else {"
      ,"		   return fib(n-1)+fib(n-2);"
      ,"        }"
      ,"	}"
      ,"}"
    )
  );

  private static final String THREE = Joiner.on("\n").join(
    ImmutableList.of(
      "public class FibonacciIterative {"
      ,"	public static int fib(int n) {"
      ,"      int prev1=0, prev2=1;"
      ,"      for(int i=0; i<n; i++) {"
      ,"          int savePrev1 = prev1;"
      ,"          prev1 = prev2;"
      ,"          prev2 = savePrev1 + prev2;"
      ,"      }"
      ,"      return prev1;"
      ,"	}"
      ,""
      ,"}"
    )
  );

  private static final String FOUR = Joiner.on("\n").join(
    ImmutableList.of(
      "import java.math.BigInteger;"
      ,"import java.util.ArrayList;"
      ,""
      ,"public class FibonacciMemoized {"
      ,"        "
      ,"	private static ArrayList<BigInteger> fibCache = new ArrayList<BigInteger>();"
      ,"	static {"
      ,"      fibCache.add(BigInteger.ZERO);"
      ,"      fibCache.add(BigInteger.ONE);"
      ,"	}"
      ,""
      ,"    public static BigInteger fib(int n) {"
      ,"       if (n >= fibCache.size()) {"
      ,"           fibCache.add(n, fib(n-1).add(fib(n-2)));"
      ,"        }"
      ,"		"
      ,"        return fibCache.get(n);"
      ,"    }"
      ,"}"
    )
  );

  private static final String FIVE = Joiner.on("\n").join(
    ImmutableList.of(
      "public class CrunchifyFibonacci {"
      ," "
      ,"    // Java program for Fibonacci number using Loop."
      ,"    public static int fibonacciLoop(int number){"
      ,"        if(number == 1 || number == 2){"
      ,"            return 1;"
      ,"        }"
      ,"		"
      ,"        int fibo1=1, fibo2=1, fibonacci=1;"
      ,"        for(int i= 3; i<= number; i++){"
      ,"            fibonacci = fibo1 + fibo2;"
      ,"            fibo1 = fibo2;"
      ,"            fibo2 = fibonacci;"
      ," "
      ,"        }"
      ,"		"
      ,"        return fibonacci; //Fibonacci number"
      ,"    }"
      ,"}"
    )
  );

  private static final String SIX = Joiner.on("\n").join(
    ImmutableList.of(
      "class Fibonacci {"
      ,"    /*"
      ,"     * Java program to calculate Fibonacci number using loop or Iteration."
      ,"     * @return Fibonacci number"
      ,"     */"
      ,"    public static int fibonacci2(int number){"
      ,"        if(number == 1 || number == 2){"
      ,"            return 1;"
      ,"        }"
      ,"        int fibo1=1, fibo2=1, fibonacci=1;"
      ,"        for(int i= 3; i<= number; i++){"
      ,"           "
      ,"            //Fibonacci number is sum of previous two Fibonacci number"
      ,"            fibonacci = fibo1 + fibo2;             "
      ,"            fibo1 = fibo2;"
      ,"            fibo2 = fibonacci;"
      ,"          "
      ,"        }"
      ,"        return fibonacci; //Fibonacci number"
      ,"      "
      ,"    }"
      ,"}"
    )
  );

  static Source one(){
    return Source.from("Fibonacci", ONE);
  }

  static Source two(){
    return Source.from("Fibonacci", TWO);
  }

  static Source three(){
    return Source.from("FibonacciIterative", THREE);
  }

  static Source four(){
    return Source.from("FibonacciMemoized", FOUR);
  }

  static Source five(){
    return Source.from("CrunchifyFibonacci", FIVE);
  }

  static Source six(){
    return Source.from("Fibonacci", SIX);
  }

  static List<Source> corpus(){
    return Lists.newArrayList(one(), two(), three(), four(), five(), six());
  }

}
