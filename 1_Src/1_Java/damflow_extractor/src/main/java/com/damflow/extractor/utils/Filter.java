package com.damflow.extractor.utils;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.jordansamhi.androspecter.files.SystemManager;

import soot.SootMethod;

public class Filter {

    // Name of Sources/Sinks files
    public static final String ANDROID_API_FILE = "AndroidAPIs.txt";
    
    // Load the list of methods from the text file in the resources folder
    private static Set<String> loadMethodNames() {
        Set<String> methodNames = new HashSet<>();
        try (InputStream inputStream = Filter.class.getClassLoader().getResourceAsStream(ANDROID_API_FILE);
             BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream))) {
            String line;
            while ((line = reader.readLine()) != null) {
                methodNames.add(line.trim());
            }
        } 
        catch (Exception e) {
            e.printStackTrace();
        }
        return methodNames;
    }

    // Filter out methods that are not in the loaded list
    public static Set<SootMethod> filterNonSystemLibraries(Set<SootMethod> appMethods) {

        // Method names loaded from the text file
        Set<String> androidApiMethods = loadMethodNames();
        // Iterate 
        Iterator<SootMethod> iterator = appMethods.iterator();
        while (iterator.hasNext()) {
            SootMethod method = iterator.next();

            // Remove methods not in the list
            if (!androidApiMethods.contains(method.getSignature())) {
                iterator.remove();
            }
        }
        //System.out.println("--- #️⃣  Filter: Non-System Library Methods (NEW) --> " + appMethods.size());
        return appMethods;
    }
}
    
