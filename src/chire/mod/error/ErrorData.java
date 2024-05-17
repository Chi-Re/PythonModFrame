package chire.mod.error;

import arc.util.Strings;
import chire.mod.ModData;
import chire.mod.PyMods;

import static chire.PythonJavaMod.pyMods;

public class ErrorData {
    public ModData config;

    public Throwable throwable;

    public boolean fatal;

    public ErrorData(ModData config, Throwable throwable, boolean fatal){
        this.config = config;
        if (fatal) {
            this.throwable = new Throwable("Mod " + config.name +
                    " º”‘ÿ ß∞‹£¨÷¬√¸¥ÌŒÛÃ¯≥ˆ\n\n[Python]"+throwable.toString() +
                    "\n\n[java]" + Strings.neatError(throwable));
        } else {
            this.throwable = new Throwable("Mod " + config.name +
                    " º”‘ÿ ß∞‹£¨Ã¯π˝º”‘ÿ\n\n[Python]\n"+throwable.toString() +
                    "\n\n[java]\n" + Strings.neatError(throwable));
        }
        this.fatal = fatal;
        PyMods.hasError = true;
    }

    @Override
    public String toString() {
        return "ErrorData{" +
                "config=" + config +
                ", throwable=" + throwable.getMessage() +
                ", fatal=" + fatal +
                '}';
    }
}
