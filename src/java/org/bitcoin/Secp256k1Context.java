/*
 * Copyright 2014-2016 the libsecp256k1 contributors
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.bitcoin;

import fr.hugo4715.oslib.AbstractOperatingSystem;
import fr.hugo4715.oslib.Arch;
import fr.hugo4715.oslib.OperatingSystem;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.Field;
import java.util.Arrays;

/**
 * This class holds the context reference used in native methods 
 * to handle ECDSA operations.
 */
@SuppressWarnings("unused")
public class Secp256k1Context {
    private static final Logger logger = LoggerFactory.getLogger(Secp256k1Context.class);
    private static final boolean enabled; //true if the library is loaded
    private static final long context; //ref to pointer to context obj

    static { //static initializer
        boolean isEnabled = true;
        long contextRef = -1;
        try {
            importLibraryPath();
            System.loadLibrary("secp256k1");
            contextRef = secp256k1_init_context();
        } catch (UnsatisfiedLinkError e) {
            logger.error("UnsatisfiedLinkError: " + e.toString());
            isEnabled = false;
        } catch (Exception e) {
            logger.error("Exception: " + e.toString());
            e.printStackTrace();
        }
        enabled = isEnabled;
        context = contextRef;
    }

    public static boolean isEnabled() {
        return enabled;
    }

    public static long getContext() {
        if(!enabled) return -1; //sanity check
        return context;
    }

    private static native long secp256k1_init_context();

    private static void importLibraryPath() throws Exception {
        AbstractOperatingSystem os = OperatingSystem.getOperatingSystem();
        if (os.getType() == OperatingSystem.OSX) {
            addLibraryPath("./libs/mac_x64");
            return;
        } else if (os.getType() == OperatingSystem.LINUX) {
            if (os.getArch() == Arch.x86_64) {
                addLibraryPath("./libs/linux_x64");
                return;
            }
        }

        throw new Exception("secp256k1 native library not available on platform " + os.getType().toString());
    }

    /**
     * Adds the specified path to the java library path
     *
     * @param pathToAdd the path to add
     * @throws Exception
     */
    private static void addLibraryPath(String pathToAdd) throws Exception{
        final Field usrPathsField = ClassLoader.class.getDeclaredField("usr_paths");
        usrPathsField.setAccessible(true);

        //get array of paths
        final String[] paths = (String[])usrPathsField.get(null);

        //check if the path to add is already present
        for(String path : paths) {
            if(path.equals(pathToAdd)) {
                return;
            }
        }

        //add the new path
        final String[] newPaths = Arrays.copyOf(paths, paths.length + 1);
        newPaths[newPaths.length-1] = pathToAdd;
        usrPathsField.set(null, newPaths);
    }
}