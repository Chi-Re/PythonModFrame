package chire.mod;

import arc.Core;
import arc.util.ArcRuntimeException;
import chire.mod.error.ErrorData;
import org.python.core.PyFunction;
import org.python.util.PythonInterpreter;

import static chire.PythonJavaMod.*;

/**关于模组的解释器*/
public class ModInterpreter extends InterpreterOperation implements General {
    private final PythonInterpreter interpreter;

    private final ModData config;

    private PyFunction init = null;

    private PyFunction load = null;

    public ModInterpreter(ModData data, PythonInterpreter inter){
        interpreter = inter;
        config = data;
    }

    @Override
    public void run() {
        //interpreter.exec("import sys");
        //interpreter.exec("sys.path.append('"+ config.directory.path() + "')");

        interpreter.exec(importFile(config.directory.child(config.main), interpreter));

        init = getFun("init", interpreter);
        load = getFun("load", interpreter);
    }

    @Override
    public void runInit() {
        if (init != null) init.__call__();
    }

    @Override
    public void runLoad() {
        if (load != null) load.__call__();
    }

    @Override
    public void addSprite(){
        var sprite = config.directory.child("sprites");
        var override = config.directory.child("sprites-override");

        try {
            spriteOperation(sprite, true);
            spriteOperation(override, false);
        } catch (Throwable e){
            pyMods.addError(new ErrorData(config, e, e instanceof ArcRuntimeException));
        }
    }

    @Override
    public void addBundle() {
        var bundlesDirectory = config.directory.child("bundles");

        try {
            bundleOperation(bundlesDirectory);
        } catch (Throwable e){
            pyMods.addError(new ErrorData(config, e, e instanceof ArcRuntimeException));
        }
    }

    public ModData getConfig() {
        return config;
    }

    @Override
    public void close() {
        interpreter.cleanup();
    }
}
