package org.petero.cuckoo.engine.chess;

import java.util.ArrayList;
import java.util.Map;
import java.util.TreeMap;

public class Parameters {
    public enum Type {
        CHECK,
        SPIN,
        COMBO,
        BUTTON,
        STRING
    }

    public static class ParamBase {
        protected String name;
        protected Type type;
        protected boolean visible;

        public String getName() {
            return name;
        }

        public Type getType() {
            return type;
        }
    }

    public static final class CheckParam extends ParamBase {
        public final boolean defaultValue;
        CheckParam(String name, boolean visible, boolean def) {
            this.name = name;
            this.type = Type.CHECK;
            this.visible = visible;
            this.defaultValue = def;
        }
    }

    public static final class SpinParam extends ParamBase {
        public final int minValue;
        public final int maxValue;
        private int value;
        public final int defaultValue;
        SpinParam(String name) {
            this.name = name;
            this.type = Type.SPIN;
            this.visible = false;
            this.minValue = -200;
            this.maxValue = 200;
            this.value = 0;
            this.defaultValue = 0;
        }
    }

    public static final class ComboParam extends ParamBase {
        public final String[] allowedValues;
        public final String defaultValue;
        ComboParam(String name, boolean visible, String[] allowed, String def) {
            this.name = name;
            this.type = Type.COMBO;
            this.visible = visible;
            this.allowedValues = allowed;
            this.defaultValue = def;
        }
    }

    public static final class StringParam extends ParamBase {
        public final String defaultValue;
        StringParam(String name, boolean visible, String def) {
            this.name = name;
            this.type = Type.STRING;
            this.visible = visible;
            this.defaultValue = def;
        }
    }

    public static Parameters instance() {
        return inst;
    }
    public final String[] getParamNames() {
        ArrayList<String> parNames = new ArrayList<>();
        for (Map.Entry<String, ParamBase> e : params.entrySet())
            if (e.getValue().visible)
                parNames.add(e.getKey());
        return parNames.toArray(new String[0]);
    }

    public final ParamBase getParam(String name) {
        return params.get(name);
    }

    private static final Parameters inst = new Parameters();
    private final Map<String, ParamBase> params = new TreeMap<>();

    private Parameters() {
        addPar(new SpinParam("qV"));
        addPar(new SpinParam("rV"));
        addPar(new SpinParam("bV"));
        addPar(new SpinParam("nV"));
        addPar(new SpinParam("pV"));
    }

    private void addPar(ParamBase p) {
        params.put(p.name.toLowerCase(), p);
    }

    final int getIntPar(String name) {
        return ((SpinParam)params.get(name.toLowerCase())).value;
    }

    public final void set(String name, String value) {
        ParamBase p = params.get(name.toLowerCase());
        if (p == null)
            return;
        switch (p.type) {
        case CHECK, BUTTON, STRING:
            break;
        case SPIN: {
            SpinParam sp = (SpinParam)p;
            try {
                int val = Integer.parseInt(value);
                if ((val >= sp.minValue) && (val <= sp.maxValue))
                    sp.value = val;
            } catch (NumberFormatException ignored) {
            }
            break;
        }
        case COMBO: {
            ComboParam cp = (ComboParam)p;
            for (String allowed : cp.allowedValues)
                if (allowed.equalsIgnoreCase(value)) {
                    break;
                }
            break;
        }
        }
    }
}
