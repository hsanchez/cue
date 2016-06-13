package com.vesperin.cue.utils;

import com.google.common.base.Splitter;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * @author Huascar Sanchez
 */
class Signatures {
  private Signatures(){
    throw new Error("Cannot be instantiated");
  }

  /**
   * Checks if the array of strings contains a single element. We expect two
   * @param parsedElements the parsing output.
   * @return true if
   */
  static boolean isSingle(String[] parsedElements){
    return parsedElements != null && parsedElements.length == 1;
  }

  /**
   * Parses a method entry and returns an array containing
   * the elements of the entry.
   *
   * @param entry method entry
   * @return array representation of signature.
   */
  static String[] entrySignature(String entry) {
    final List<String> extracted = new ArrayList<>();

    final String cleaned = cleans(entry.trim());

    if(cleaned.indexOf("::") > 0){
      final String[] parts = entry.split("::");

      Collections.addAll(extracted, parts);

    } else {
      extracted.add(cleaned);
    }

    return extracted.toArray(new String[extracted.size()]);
  }


  /**
   * Parses a method signature.
   *
   * @param methodCallSignature method signature to parse.
   * @return an array of elements in a signature.
   */
  static String[] methodSignature(String methodCallSignature) {
    final List<String> extracted = new ArrayList<>();
    String methodName = methodCallSignature;
    String methodCallDesc = null;

    if (methodCallSignature.indexOf("(") > 0) {
      methodName      = methodName.substring(0, methodCallSignature.indexOf("("));
      methodCallDesc  = methodCallSignature
        .substring(
          methodCallSignature.indexOf("(") + 1,
          methodCallSignature.lastIndexOf(")")
        );
    }

    extracted.add(methodName);

    if (methodCallDesc != null) {
      final Iterable<String> parameters = Splitter.on(",").split(methodCallDesc);
      for(String eachParam : parameters){
        final List<String> parameterInfo = Splitter.on(" ").splitToList(
          replaceSubString(cleans(eachParam.trim()), "  ", " ")
        );

        if(!parameterInfo.isEmpty() && parameterInfo.get(0).contains("(")){
          final Matcher matcher = Pattern.compile("\\(([^)]+)\\)").matcher(parameterInfo.get(0));

          if(matcher.find()){
            final String type = matcher.group(1);
            final String receiver = parameterInfo.get(0)
              .substring(
                parameterInfo.get(0).lastIndexOf(")") + 1,
                parameterInfo.get(0).length()
              );

            extracted.add(type); // type is first
            extracted.add(receiver);
          }

        } else {
          extracted.addAll(parameterInfo);
        }

      }

    }

    return extracted.toArray(new String[extracted.size()]);
  }


  /**
   * Removes newline, carriage return and tab characters from a string.
   *
   * @param toBeEscaped string to escape
   * @return the escaped string
   */
  private static String cleans(final String toBeEscaped) {
    final StringBuilder escapedString = new StringBuilder();
    for (int i = 0; i < toBeEscaped.length(); i++) {
      if ((toBeEscaped.charAt(i) != '\n')
        && (toBeEscaped.charAt(i) != '\r')
        && (toBeEscaped.charAt(i) != '\t')) {

        escapedString.append(toBeEscaped.charAt(i));
      }
    }

    return escapedString.toString();
  }

  /**
   * Replaces all occurrences of a substring inside a string.
   *
   * @param str      the string to search and replace in
   * @param oldToken the string to search for
   * @param newToken the string to replace newToken
   * @return the new string
   */
  private static String replaceSubString(final String str, final String oldToken, final String newToken) {
    return replaceSubString(str, oldToken, newToken, -1);
  }

  /**
   * Replaces all occurrences of a substring inside a string.
   *
   * @param str      the string to search and replace in
   * @param oldToken the string to search for
   * @param newToken the string to replace newToken
   * @param max      maximum number of values to replace (-1 => no maximum)
   * @return the new string
   */
  private static String replaceSubString(final String str, final String oldToken, final String newToken, int max) {
    if ((str == null) || (oldToken == null) || (newToken == null) || (oldToken.length() == 0)) {
      return str;
    }

    final StringBuilder stringBuilder = new StringBuilder(str.length());
    int start = 0;
    int end;

    while ((end = str.indexOf(oldToken, start)) != -1) {
      stringBuilder.append(str.substring(start, end)).append(newToken);
      start = end + oldToken.length();
      if (--max == 0) {
        break;
      }
    }

    stringBuilder.append(str.substring(start));

    return stringBuilder.toString();
  }

}
