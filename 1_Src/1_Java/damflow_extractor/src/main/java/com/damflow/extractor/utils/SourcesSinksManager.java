package com.damflow.extractor.utils;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import soot.SootMethod;
import soot.jimple.infoflow.android.data.AndroidMethod;

public class SourcesSinksManager {
    
    // Name of Sources/Sinks files
    public static final String ONLY_SINKS_FILE  = "onlysinksList.txt";
    public static final String DOCFLOW__FILE    = "docflowList.txt";

    
    // Sources and Sinks Sets
    public static Set<AndroidMethod> sources = new HashSet<>();
    public static Set<AndroidMethod> sinks   = new HashSet<>();


    public static void loadSourcesAndSinks(String approach) {

        // Default Option
        String LIST_TO_BE_USED = DOCFLOW__FILE; 

        if ("docflow".equals(approach)) {
            LIST_TO_BE_USED = DOCFLOW__FILE;
        }

        //Read all the lines of the file
        try (InputStream input = SourcesSinksManager.class.getClassLoader().getResourceAsStream(LIST_TO_BE_USED);
            BufferedReader reader = new BufferedReader(new InputStreamReader(input))) {

            String line;
            while ((line = reader.readLine()) != null) {
                addSourceSinkFromMethodSignature(line);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }


    public static void loadSourcesAndSinksBackwardAnalysis(Set<SootMethod> possibleSources) {
        // Read Sinks from File
        try (InputStream input = SourcesSinksManager.class.getClassLoader().getResourceAsStream(ONLY_SINKS_FILE);
            BufferedReader reader = new BufferedReader(new InputStreamReader(input))) {

            String line;
            while ((line = reader.readLine()) != null) {
                addSourceSinkFromMethodSignature(line);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }

        // Add possible sources
        for (SootMethod source : possibleSources) {
            String methodSignature = source.toString() + " -> _SOURCE_";
            addSourceSinkFromMethodSignature(methodSignature);
        }

        // Remove sources that are also sinks:
        sources.removeAll(sinks);

    }



    private static void addSourceSinkFromMethodSignature(String methodString) {
        // Skip the method if it starts with "%"
        if (methodString.trim().startsWith("%")) {
            return;
        }

        // Split the methodString into method signature and type
        String[] split = methodString.split("->");

        if (split.length == 2) {
            String methodSignature = split[0].trim();
            String type = split[1].trim();

            try {
                // Create an Android Method object using signature components
                AndroidMethod androidMethod = new AndroidMethod(
                    getMethodNameFromSignature(methodSignature),
                    getParametersNamesFromSignature(methodSignature),
                    getReturnNameFromSignature(methodSignature),
                    getClassNameFromSignature(methodSignature)
                );

                // Add source, sink, or both based on type
                if ("_SOURCE_".equals(type)) {
                    sources.add(androidMethod);
                } else if ("_SINK_".equals(type)) {
                    sinks.add(androidMethod);
                } else if ("_BOTH_".equals(type)) {
                    sources.add(androidMethod);
                    sinks.add(androidMethod);
                }

            } 
            catch (Exception e) {
                return;
            }

        }
    }

    /**
     * Retrieves the class name from a signature string.
     * 
     * @param sig The signature string from which the class name is to be extracted.
     * @return The class name extracted from the signature string.
     */
    public static String getClassNameFromSignature(String sig) {
        // Split the signature string and extract the class name
        String tmp = sig.split(" ")[0];
        // Remove leading and trailing characters to get the class name
        return tmp.substring(1, tmp.length() - 1);
    }

    /**
     * Retrieves the method name from a signature string.
     * 
     * @param sig The signature string from which the method name is to be extracted.
     * @return The method name extracted from the signature string.
     */
    public static String getMethodNameFromSignature(String sig) {
        // Split the signature string and extract the method name
        String tmp = sig.split(" ")[2];
        // Extract the method name before the opening parenthesis
        return tmp.substring(0, tmp.indexOf("("));
    }

    /**
     * Retrieves the return type name from a signature string.
     * 
     * @param sig The signature string from which the return type name is to be extracted.
     * @return The return type name extracted from the signature string.
     */
    public static String getReturnNameFromSignature(String sig) {
        // Split the signature string and extract the return type name
        return sig.split(" ")[1];
    }

    /**
     * Retrieves the parameter names from a method signature string.
     * 
     * @param sig The method signature string from which parameter names are to be extracted.
     * @return A list of parameter names extracted from the method signature.
     */
	public static List<String> getParametersNamesFromSignature(String sig) {
		String tmp = sig.split(" ")[2];
		String params = tmp.substring(tmp.indexOf("(") + 1, tmp.indexOf(")"));
		String[] paramsArray = params.split(",");
		List<String> parameters = new ArrayList<String>();
		for(int i = 0 ; i < paramsArray.length ; i++) {
			parameters.add(paramsArray[i]);
		}
		return parameters;
	}
}
