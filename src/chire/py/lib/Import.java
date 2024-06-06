package chire.py.lib;

import org.python.core.PyObject;

import static chire.mod.PyMods.imports;

/**
 * 为了在代码中import而设计的
 */
public class Import {
    public static PyObject getPyVar(String path, String name){
        return imports.get(path).get(name);
    }

//    public static PyFunction getPyFun(String path, String name){
//        importInter.execfile(path);
//        return (PyFunction) importInter.get(name);
//    }
//
//    public static PyClass getPyClass(String path, String name){
//        importInter.execfile(path);
//        return (PyClass) importInter.get(name);
//    }
}
