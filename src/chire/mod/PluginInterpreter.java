package chire.mod;

import arc.files.Fi;
import arc.util.ArcRuntimeException;
import chire.mod.error.ErrorData;
import org.python.core.PyFunction;
import org.python.util.PythonInterpreter;

import java.io.IOException;

import static chire.PythonJavaMod.*;

/**关于插件的解释器代码*/
public class PluginInterpreter extends FileOperation implements General{
    private final Fi pluginDirectory;

    private final PythonInterpreter interpreter;

    private PyFunction init;

    private PyFunction load;

    private final ModData config;

    public PluginInterpreter(ModData config){
        this.config = config;
        pluginDirectory = config.directory;
        pluginDirectory.mkdirs();

        interpreter = new PythonInterpreter();
    }

    @Override
    public void run() throws IOException {
        interpreter.exec("import sys");
        interpreter.exec("sys.path.append('" + pluginDirectory.path() + "')");

        Fi mainFile = pluginDirectory.child("main.py");

        if (!mainFile.exists()) mainFile.file().createNewFile();

        interpreter.execfile(mainFile.path());

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
    public void addSprite() {
        var spriteDirectory = pluginDirectory.child("sprites");
        var overridesDirectory = pluginDirectory.child("sprites-override");
        spriteDirectory.mkdirs();
        overridesDirectory.mkdirs();

        spriteOperation(
                spriteDirectory.findAll(f -> f.extension().equals("png")),
                true
        );
        spriteOperation(
                overridesDirectory.findAll(f -> f.extension().equals("png")),
                false
        );
    }

    @Override
    public void addBundle() {
        var bundlesDirectory = pluginDirectory.child("bundles");
        bundlesDirectory.mkdirs();

        try {
            bundleOperation(bundlesDirectory);
        } catch (Throwable e){
            errors.add(new ErrorData(config, e, e instanceof ArcRuntimeException));
        }
    }

    @Override
    public ModData getConfig() {
        return config;
    }

    @Override
    public void close() {
        interpreter.cleanup();
    }
}
