package chire.mod;

import arc.files.Fi;
import arc.struct.ArrayMap;
import arc.util.Log;
import chire.serialization.CRJson;
import org.python.core.Py;
import org.python.core.PyObject;
import org.python.util.PythonInterpreter;

import java.util.ArrayList;
import java.util.Objects;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public abstract class InterpreterOperation extends FileOperation {
//    /**
//     * 这是个特殊的import方法，为了适配手机无法加载jython的sys
//     *
//     * @param pyCode 指定的py文件
//     * @return 解析后的代码字符串。
//     */
//    public String importFile(Fi pyCode){
//        if (pyCode.isDirectory()) throw new RuntimeException("文件 "+pyCode+" 为文件夹，加载出错。");
//
//        PythonInterpreter importInter = new PythonInterpreter();
//        importInter.exec("from chire.py.lib import Import");
//        String code = pyCode.readString();
//        for (var s : code.split("\n")){
//            //String regex = "(?i)\\s*from\\s+\\w+(?:\\.\\w+)*\\s+import\\s+\\w+(?:\\.\\w+)*(?:,\\s*\\w+(?:\\.\\w+)*)*";
//            Pattern fi = Pattern.compile("^(?i)\\s*from\s+(.*?)\s+import\s+(.*?)$");
//            Pattern i = Pattern.compile("^(?i)\\s*import\s+(.*?)$");
//            Matcher mat = fi.matcher(s);
//
//            String filePath = null;
//            if (mat.find()) {
//                filePath = mat.group(1)+"."+mat.group(2);
//            } else {
//                Matcher mati = i.matcher(s);
//                if (mati.find()) filePath = mati.group(1);
//            }
//            if (filePath == null) continue;
//
//            Log.info(filePath);
//            try {
//                //判断是否为java内部类
//                Class.forName(filePath);
//            } catch (NullPointerException | ClassNotFoundException e){
//                if (filePath.contains("*")) {
//                    filePath = filePath.replace(".", "/").replace("/*", "")+".py";
//                    Fi file = pyCode.parent().child(filePath);
//                    //if (file.isDirectory()) file = file.child("__init__.py");
//                    Log.info(file);
//                    if (!file.exists()) continue;
//
//                    importInter.execfile(file.path());
//                    ArrayMap<String, Class<?>> imports = new ArrayMap<>();
//                    for (var l : new CRJson(
//                            importInter.getLocals().toString().replaceAll("'", "")
//                    ).parse()){
//                        if (
//                                Objects.equals(l.name(), "__doc__") ||
//                                Objects.equals(l.name(), "__name__") ||
//                                Objects.equals(l.name(), "Import")
//                        ) continue;
//                        Log.info(l.name());
//                        imports.put(l.name, importInter.get(l.name()).getClass());
//                    }
//                    Log.info(imports);
//
//                    for (int j2 = 0; j2 < imports.size; j2++) {
//                        Log.info("c:"+imports.getKeyAt(0)+" i:"+imports.getKeyAt(j2));
//                        Log.info(isInstance(importInter.get(imports.getKeyAt(0)), importInter.get(imports.getKeyAt(j2))));
//                        if (isInstance(importInter.get(imports.getKeyAt(0)), importInter.get(imports.getKeyAt(j2))))
//                            imports.put(imports.getKeyAt(j2), imports.getValueAt(j2), 0);
//                        Log.info(imports);
//                    }
//
//                    code = code.replace(s, analysisImport(imports, file.path()));
//                } else {
//                    var varName = filePath.substring(filePath.lastIndexOf(".")+1);
//                    filePath = pyCode.parent().child(filePath.substring(
//                            0, filePath.lastIndexOf(".")
//                    ).replaceAll("\\.", "/")).path()+".py";
//                    code = code.replace(s, varName+"=Import.getPyVar('"+filePath+"','"+varName+"')");
//                }
//            }
//        }
//        importInter = null;
//        Log.info(code);
//        return code;
//    }

    public String importFile(Fi pyCode, PythonInterpreter interpreter){
        Log.info("dsd:"+pyCode);
        if (pyCode.isDirectory()) throw new RuntimeException("文件 "+pyCode+" 为文件夹，加载出错。");

        String code = pyCode.readString();
        for (var s : code.split("\n")) {
            Pattern fi = Pattern.compile("^(?i)\\s*from\s+(.*?)\s+import\s+(.*?)$");
            Pattern i = Pattern.compile("^(?i)\\s*import\s+(.*?)$");
            Matcher mat = fi.matcher(s);

            String filePath = null;
            String importName = null;
            if (mat.find()) {
                filePath = mat.group(1);
                importName = mat.group(2);
            } else {
                Matcher mati = i.matcher(s);
                if (mati.find()) filePath = mati.group(1);
            }
            if (filePath == null && importName == null) continue;

            try {
                //判断是否为java内部类
                Class.forName(filePath+"."+importName);
            } catch (NullPointerException | ClassNotFoundException e) {
                PythonInterpreter inter = new PythonInterpreter();
                if (importName == null) {
                    //比如 import dsad.dsad
                } else if (importName.equals("*")) {
                    Fi file = getImportFi(pyCode.parent(), filePath);
                    if (!file.exists()) continue;

                    inter.exec(importFile(file, inter));
                    code = code.replace(s, "");

                    for (var l : new CRJson(
                            inter.getLocals().toString().replaceAll("'", "")
                    ).parse()) {
                        if (
                                Objects.equals(l.name(), "__doc__") ||
                                Objects.equals(l.name(), "__name__")
                        ) continue;
                        Log.info(l.name());
                        interpreter.set(l.name(), inter.get(l.name()));
                    }
                } else {
                    Log.info("varName:"+importName);
                    inter.exec(importFile(getImportFi(pyCode.parent(), filePath), inter));
                    code = code.replace(s, "");

                    interpreter.set(importName, inter.get(importName));
                }
                Log.info(code);
            }
        }
        return code;
    }

    protected String analysisImport(ArrayMap<String, Class<?>> importCodes, String path){
        StringBuilder vars = new StringBuilder();
        StringBuilder funs = new StringBuilder();
        for (var ic : importCodes.keys()) {
            vars.append(ic);
            vars.append(",");
            funs.append("Import.");
            funs.append("getPyVar('").append(path).append("', '").append(ic).append("')");
            //funs.append("Import('").append(path).append("', '").append(ic).append("')");
            funs.append(",");
        }
        if (vars.length() > 0 && funs.length() > 0) return vars.deleteCharAt(vars.length()-1)
                .append("=")
                .append(funs.deleteCharAt(funs.length()-1))
                .toString();
        return "";
    }

    protected boolean isInstance(PyObject cl, PyObject in){
        return cl != in && Py.isSubClass(cl, in);
    }

    protected Fi getImportFi(Fi importDirectory, String line){
        String path = importDirectory.path();
        line = line.replaceAll("\\.", "/");
        String s = line;
        while (true){
//            Log.info("这是循环");
//            Log.info("path:"+path);
//            Log.info("line:"+line);
//            Log.info("s:"+s);
            if (path.contains(line)) {
                return importDirectory.child("__init__.py");
            }

            if (path.contains(s)) {
                var fi = importDirectory.child(line.replace(s+"/", ""));
                if (fi.isDirectory() && fi.exists()) {
                    fi = fi.child("__init__.py");
                } else {
                    fi = new Fi(fi.path()+".py");
                }

                if (!fi.exists()) throw new RuntimeException("文件不存在");
                return fi;
            }

            if (!s.contains("/")) {
                var fi = importDirectory.child(line);

                if (fi.isDirectory() && fi.exists()) {
                    fi = fi.child("__init__.py");
                } else {
                    fi = new Fi(fi.path()+".py");
                }

                if (!fi.exists()) throw new RuntimeException("文件不存在");
                return fi;
            }

            s = s.substring(0, s.lastIndexOf("/"));
        }
    }
}
