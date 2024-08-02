package com.damflow.extractor.utils;

import java.util.Iterator;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.jordansamhi.androspecter.files.SystemManager;

import soot.SootMethod;

public class Filter {
    
    // Manager for system Libraries
    static SystemManager systemManager = SystemManager.v();

    public static Set<SootMethod> filterNonSystemLibraries(Set<SootMethod> appMethods) {
        // Iterate over the set of methods
        Iterator<SootMethod> iterator = appMethods.iterator();
        while (iterator.hasNext()) {
            SootMethod method = iterator.next();
            if (!systemManager.isSystemClass(method.getDeclaringClass())) {
                iterator.remove();
                continue;
            }
            if (method.getName().contains("<init>")) {
                iterator.remove();
                continue;
            }
            Pattern voidPattern = Pattern.compile("\\svoid\\s");
            Matcher matcher = voidPattern.matcher(method.getSignature());
            if (matcher.find()) {
                iterator.remove();
            }
        }
    

        System.out.println("--- #️⃣  Filter: --> " + appMethods.size());
        return appMethods;
    }
}
    
